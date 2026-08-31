// WASM prelude. Keep this independent from native resources so target runtimes can diverge.
mod errable;
mod error;
pub use errable::Errable;
pub use error::{nil, VajramError};

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
