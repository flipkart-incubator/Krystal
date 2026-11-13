// Part of the keka_rt prelude bundled with keka-rust-compiler. Copied verbatim into
// every generated crate's `src/keka_rt/` - see RustCompilerMain.

use std::fmt;

/// The error type behind every keka errable (`T?`) facet, mapped to `Result<T, VajramError>`.
///
/// It is `PartialEq`, `Eq` and `Hash` so that models with errable fields can be compared and hashed.
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct VajramError {
    pub message: String,
    /// True only for keka's `nil` (an absent value), as opposed to a real error.
    pub nil: bool,
}

impl VajramError {
    pub fn new(message: impl Into<String>) -> Self {
        Self {
            message: message.into(),
            nil: false,
        }
    }

    pub fn is_nil(&self) -> bool {
        self.nil
    }

    /// Backs keka's `nil` literal - see the free function `nil()` below.
    pub fn nil() -> Self {
        Self {
            message: "nil".to_string(),
            nil: true,
        }
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

/// Generic stand-in for keka's `nil` literal; `T` is inferred from the call site since
/// Rust has no single nameable "nil of any type" value the way keka does.
pub fn nil<T>() -> Result<T, VajramError> {
    Err(VajramError::nil())
}
