// WASM prelude. Keep this independent from native resources so target runtimes can diverge.
mod errable;
mod error;
pub use errable::Errable;
pub use error::{nil, VajramError};

use std::rc::Rc;
use std::{any::Any, cell::RefCell, collections::HashMap};

/// Per-vajram-call-chain bump arena - see the native prelude's doc comment for the full rationale.
pub type Arena = bumpalo::Bump;

#[wasm_bindgen::prelude::wasm_bindgen]
extern "C" {
    #[wasm_bindgen::prelude::wasm_bindgen(js_name = emit_vajram_output)]
    fn emit_vajram_output(message: &str);
}

#[derive(Clone, Debug, Eq, Hash, PartialEq)]
pub struct InjectionKey {
    pub type_name: String,
    pub selectors: Vec<String>,
}

impl InjectionKey {
    pub fn new(type_name: &str, selectors: &[&str]) -> Self {
        Self { type_name: type_name.to_owned(), selectors: selectors.iter().map(|s| (*s).to_owned()).collect() }
    }
}

pub trait Provider<T: ?Sized> {
    fn get(&self) -> Rc<T>;
}

/// See the native prelude: `AppContext` is now threaded by plain reference, not `Rc`.
pub trait Injector: Sized {
    fn get_provider<T: ?Sized + 'static>(&self, key: InjectionKey, context: &AppContext<Self>) -> Rc<dyn Provider<T>>;
}

pub struct DefaultInjector {
    providers: HashMap<InjectionKey, Rc<dyn Any>>,
}

struct StdOutProvider;

impl Provider<dyn ConsoleWriter> for StdOutProvider {
    fn get(&self) -> Rc<dyn ConsoleWriter> { Rc::new(StdOut) }
}

impl Default for DefaultInjector {
    fn default() -> Self {
        let mut providers: HashMap<InjectionKey, Rc<dyn Any>> = HashMap::new();
        let provider: Rc<dyn Provider<dyn ConsoleWriter>> = Rc::new(StdOutProvider);
        providers.insert(InjectionKey::new("lang.process.ConsoleWriter", &[]), Rc::new(provider));
        Self { providers }
    }
}

impl Injector for DefaultInjector {
    fn get_provider<T: ?Sized + 'static>(&self, key: InjectionKey, _context: &AppContext<Self>) -> Rc<dyn Provider<T>> {
        self.providers.get(&key)
            .and_then(|provider| provider.downcast_ref::<Rc<dyn Provider<T>>>())
            .cloned()
            .unwrap_or_else(|| panic!("no provider registered for injection key {:?}", key))
    }
}

pub struct AppContext<I: Injector> {
    injector: Rc<I>,
    injections: RefCell<HashMap<String, Rc<dyn Any>>>,
}

impl<I: Injector> AppContext<I> {
    pub fn new(injector: Rc<I>) -> Self { Self { injector, injections: RefCell::new(HashMap::new()) } }
    pub fn injector(&self) -> &I { self.injector.as_ref() }

    pub fn injection_instance<T: Any>(&self, key: &str, create: impl FnOnce() -> Rc<T>) -> Rc<T> {
        if let Some(instance) = self.injections.borrow().get(key) {
            return Rc::clone(instance.downcast_ref::<Rc<T>>().expect("injection key has an incompatible type"));
        }
        let instance = create();
        self.injections.borrow_mut().insert(key.to_owned(), Rc::new(Rc::clone(&instance)));
        instance
    }
}

/// Host-provided writer used by `lang.Process.ConsoleWriter` injections.
pub trait ConsoleWriter {
    fn println(&self, message: String);
}

/// Browser output implementation that delegates to the host playground callback.
pub struct StdOut;

impl ConsoleWriter for StdOut {
    fn println(&self, message: String) {
        emit_vajram_output(&message);
    }
}

/// Non-blocking HTTP GET for `lang.net.callHttp` on the wasm target, backed by the browser's
/// `fetch` API. Requires the `web_sys` crate with the `Request`, `RequestInit`, `RequestMode`,
/// `Response`, and `Window` features enabled.
pub async fn fetch_text(url: &str) -> String {
    use wasm_bindgen::JsCast;
    use wasm_bindgen_futures::JsFuture;
    use web_sys::{Request, RequestInit, RequestMode, Response};

    let opts = RequestInit::new();
    opts.set_method("GET");
    opts.set_mode(RequestMode::Cors);
    let request =
        Request::new_with_str_and_init(url, &opts).expect("callHttp failed to build request");

    let window = web_sys::window().expect("callHttp requires a browser window global");
    let response_value = JsFuture::from(window.fetch_with_request(&request))
        .await
        .expect("callHttp failed");
    let response: Response = response_value
        .dyn_into()
        .expect("callHttp received a non-Response value");
    let text_value = JsFuture::from(response.text().expect("callHttp failed to read response body"))
        .await
        .expect("callHttp failed");
    text_value.as_string().expect("callHttp response body was not a string")
}
