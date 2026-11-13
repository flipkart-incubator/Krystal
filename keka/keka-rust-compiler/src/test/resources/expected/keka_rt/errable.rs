// Part of the keka_rt prelude bundled with keka-rust-compiler. Copied verbatim into
// every generated crate's `src/keka_rt/` - see RustCompilerMain.

use crate::keka_rt::VajramError;

/// keka's `?` (errable) operator surface, implemented for `Result<T, VajramError>`.
///
/// Method names intentionally match keka's own camelCase spelling (`valuePresent`, ...)
/// rather than Rust's snake_case convention: the compiler transliterates `expr?method(...)` call
/// sites verbatim, so the trait has to line up with what it emits exactly.
#[allow(non_snake_case)]
pub trait Errable<T> {
    fn valuePresent(&self) -> bool;

    fn valueAbsent(&self) -> bool {
        !self.valuePresent()
    }

    fn errorPresent(&self) -> bool;

    fn errorAbsent(&self) -> bool {
        !self.errorPresent()
    }

    /// True only for `nil` (an absent value): neither a value nor a real error.
    fn isNil(&self) -> bool;

    fn value(&self) -> Option<T>;

    fn error(&self) -> Option<VajramError>;

    fn default(&self, fallback: T) -> T {
        self.value().unwrap_or(fallback)
    }

    /// Backs the bare `expr?` (errable-suffixed var-use) form: just surfaces `self`.
    fn as_errable(&self) -> &Self {
        self
    }
}

#[allow(non_snake_case)]
impl<T: Clone> Errable<T> for Result<T, VajramError> {
    fn valuePresent(&self) -> bool {
        self.is_ok()
    }

    fn errorPresent(&self) -> bool {
        self.is_err()
    }

    fn isNil(&self) -> bool {
        self.as_ref().err().map_or(false, |error| error.is_nil())
    }

    fn value(&self) -> Option<T> {
        self.as_ref().ok().cloned()
    }

    fn error(&self) -> Option<VajramError> {
        self.as_ref().err().cloned()
    }
}

/// Counts over a fanned-out `T?...` facet (one `Result` per element).
#[allow(non_snake_case)]
pub trait ErrableBatch {
    fn okCount(&self) -> usize;
    fn errorCount(&self) -> usize;
}

impl<T> ErrableBatch for Vec<Result<T, VajramError>> {
    fn okCount(&self) -> usize {
        self.iter().filter(|r| r.is_ok()).count()
    }

    fn errorCount(&self) -> usize {
        self.iter().filter(|r| r.is_err()).count()
    }
}
