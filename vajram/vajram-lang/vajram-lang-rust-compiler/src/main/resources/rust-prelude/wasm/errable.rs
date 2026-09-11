use crate::vajram_rt::VajramError;
#[allow(non_snake_case)]
pub trait Errable<T> {
    fn valuePresent(&self) -> bool;
    fn valueAbsent(&self) -> bool { !self.valuePresent() }
    fn errorPresent(&self) -> bool;
    fn errorAbsent(&self) -> bool { !self.errorPresent() }
    fn isNil(&self) -> bool { self.valueAbsent() && self.errorAbsent() }
    fn value(&self) -> Option<T>;
    fn error(&self) -> Option<VajramError>;
    fn default(&self, fallback: T) -> T { self.value().unwrap_or(fallback) }
    fn as_errable(&self) -> &Self { self }
}
#[allow(non_snake_case)]
impl<T: Clone> Errable<T> for Result<T, VajramError> {
    fn valuePresent(&self) -> bool { self.is_ok() }
    fn errorPresent(&self) -> bool { self.is_err() }
    fn value(&self) -> Option<T> { self.as_ref().ok().cloned() }
    fn error(&self) -> Option<VajramError> { self.as_ref().err().cloned() }
}
