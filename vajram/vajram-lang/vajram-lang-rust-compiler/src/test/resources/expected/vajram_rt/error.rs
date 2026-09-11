// Part of the vajram_rt prelude bundled with vajram-lang-rust-compiler. Copied verbatim into
// every generated crate's `src/vajram_rt/` - see RustCompilerMain.

use std::fmt;

/// The error type behind every vajram-lang errable (`T?`) facet, mapped to `Result<T, VajramError>`.
#[derive(Debug, Clone)]
pub struct VajramError {
    pub message: String,
}

impl VajramError {
    pub fn new(message: impl Into<String>) -> Self {
        Self {
            message: message.into(),
        }
    }

    /// Backs vajram-lang's `nil` literal - see the free function `nil()` below.
    pub fn nil() -> Self {
        Self::new("nil")
    }
}

impl fmt::Display for VajramError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "{}", self.message)
    }
}

impl std::error::Error for VajramError {}

impl From<&str> for VajramError {
    fn from(s: &str) -> Self {
        Self::new(s)
    }
}

impl From<String> for VajramError {
    fn from(s: String) -> Self {
        Self::new(s)
    }
}

/// Generic stand-in for vajram-lang's `nil` literal; `T` is inferred from the call site since
/// Rust has no single nameable "nil of any type" value the way vajram-lang does.
pub fn nil<T>() -> Result<T, VajramError> {
    Err(VajramError::nil())
}
