// WASM prelude. Keep this independent from native resources so target runtimes can diverge.
mod errable;
mod error;
pub use errable::Errable;
pub use error::{nil, VajramError};

use std::rc::Rc;
use std::{any::Any, cell::RefCell, collections::HashMap};

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

pub trait Injector: Sized {
    fn get_provider<T: ?Sized + 'static>(&self, key: InjectionKey, context: Rc<AppContext<Self>>) -> Rc<dyn Provider<T>>;
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
    fn get_provider<T: ?Sized + 'static>(&self, key: InjectionKey, _context: Rc<AppContext<Self>>) -> Rc<dyn Provider<T>> {
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

pub fn spawn_local<T: 'static>(future: impl std::future::Future<Output = T> + 'static) {
    wasm_bindgen_futures::spawn_local(async move {
        let _ = future.await;
    });
}

pub fn spawn_local_shared<T: Clone + 'static>(
    future: impl std::future::Future<Output = T> + 'static,
) -> impl std::future::Future<Output = T> + Clone {
    use futures::FutureExt;

    let shared = future.shared();
    spawn_local({
        let task = shared.clone();
        async move {
            let _ = task.await;
        }
    });
    shared
}
