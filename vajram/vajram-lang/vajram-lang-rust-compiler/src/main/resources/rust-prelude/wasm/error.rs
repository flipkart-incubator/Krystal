use std::fmt;
#[derive(Debug, Clone)]
pub struct VajramError { pub message: String }
impl VajramError {
    pub fn new(message: impl Into<String>) -> Self { Self { message: message.into() } }
    pub fn nil() -> Self { Self::new("nil") }
}
impl fmt::Display for VajramError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result { write!(f, "{}", self.message) }
}
impl std::error::Error for VajramError {}
impl From<&str> for VajramError { fn from(s: &str) -> Self { Self::new(s) } }
impl From<String> for VajramError { fn from(s: String) -> Self { Self::new(s) } }
pub fn nil<T>() -> Result<T, VajramError> { Err(VajramError::nil()) }
