// Part of the vajram_rt prelude bundled with vajram-lang-rust-compiler. Copied verbatim into
// every generated crate's `src/vajram_rt/` - see RustCompilerMain.

mod errable;
mod error;

pub use errable::Errable;
pub use error::{VajramError, nil};

/// Runs a generated async dependency on the current thread's Tokio LocalSet. The future owns only
/// `Rc` handles, so resolver-local values are dropped when this continuation completes.
#[cfg(feature = "tokio")]
pub async fn spawn_local<T: 'static>(future: impl std::future::Future<Output = T> + 'static) -> T {
    tokio::task::spawn_local(future)
        .await
        .expect("generated Vajram dependency task panicked or was cancelled")
}

/// Starts work on this LocalSet immediately and returns a cloneable handle for deferred facet use.
#[cfg(feature = "tokio")]
pub fn spawn_local_shared<T: Clone + 'static>(
    future: impl std::future::Future<Output = T> + 'static,
) -> impl std::future::Future<Output = T> + Clone {
    use futures::FutureExt;

    tokio::task::spawn_local(future)
        .map(|result| result.expect("generated Vajram dependency task panicked or was cancelled"))
        .shared()
}
