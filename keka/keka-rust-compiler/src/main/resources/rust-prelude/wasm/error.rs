use std::fmt;
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct VajramError { pub message: String, pub nil: bool }
impl VajramError {
    pub fn new(message: impl Into<String>) -> Self { Self { message: message.into(), nil: false } }
    pub fn is_nil(&self) -> bool { self.nil }
    pub fn nil() -> Self { Self { message: "nil".to_string(), nil: true } }
}
impl fmt::Display for VajramError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result { write!(f, "{}", self.message) }
}
impl std::error::Error for VajramError {}
impl From<&str> for VajramError { fn from(s: &str) -> Self { Self::new(s) } }
impl From<String> for VajramError { fn from(s: String) -> Self { Self::new(s) } }
pub fn nil<T>() -> Result<T, VajramError> { Err(VajramError::nil()) }
