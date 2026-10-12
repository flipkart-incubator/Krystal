// Part of the vajram_rt prelude bundled with vajram-lang-rust-compiler. Copied verbatim into
// every generated crate's `src/vajram_rt/` - see RustCompilerMain.

mod errable;
mod error;

pub use errable::Errable;
pub use error::{VajramError, nil};

use std::rc::Rc;
use std::{any::Any, cell::RefCell, collections::HashMap};

/// Per-vajram-call-chain bump arena. Every non-`local` facet value (and a Vajram's own output) is
/// allocated here instead of behind an `Rc`; the arena - and everything transient allocated into
/// it - is reclaimed in bulk when the scope that owns it (a `let arena = Arena::new();` local, or
/// one shared per top-level request) goes out of scope. Facets marked `` `local `` never touch an
/// arena at all - they're plain owned Rust values freed by ordinary drop when their Vajram returns.
pub type Arena = bumpalo::Bump;

#[derive(Clone, Debug, Eq, Hash, PartialEq)]
pub struct InjectionKey {
    pub type_name: String,
    pub selectors: Vec<String>,
}

impl InjectionKey {
    pub fn new(type_name: &str, selectors: &[&str]) -> Self {
        Self {
            type_name: type_name.to_owned(),
            selectors: selectors.iter().map(|s| (*s).to_owned()).collect(),
        }
    }
}

pub trait Provider<T: ?Sized> {
    fn get(&self) -> Rc<T>;
}

/// DI singletons (`Injector`, `Provider`, and `AppContext`'s own internals below) are process/
/// request-scoped, not per-vajram-call memory, so they intentionally stay `Rc`-based - out of
/// scope for the arena/lifetime change. `AppContext` itself is now threaded around by plain
/// reference (`&AppContext<I>`) instead of `Rc<AppContext<I>>`, since nothing spawns detached
/// tasks anymore that would need a `'static` owned handle to it.
pub trait Injector: Sized {
    fn get_provider<T: ?Sized + 'static>(&self, key: InjectionKey, context: &AppContext<Self>) -> Rc<dyn Provider<T>>;
}

pub struct DefaultInjector {
    providers: HashMap<InjectionKey, Rc<dyn Any>>,
}

struct StdOutProvider;

impl Provider<dyn ConsoleWriter> for StdOutProvider {
    fn get(&self) -> Rc<dyn ConsoleWriter> {
        Rc::new(StdOut)
    }
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
        self.providers
            .get(&key)
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
    pub fn new(injector: Rc<I>) -> Self {
        Self {
            injector,
            injections: RefCell::new(HashMap::new()),
        }
    }
    pub fn injector(&self) -> &I {
        self.injector.as_ref()
    }

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

/// Standard-output implementation of [`ConsoleWriter`].
pub struct StdOut;

impl ConsoleWriter for StdOut {
    fn println(&self, message: String) {
        println!("{}", message);
    }
}
