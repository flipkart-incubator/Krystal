mod support;

#[test]
fn runs_hello_world() {
    support::assert_vajram_output("helloWorld", &[], "Hello from vajram-lang!");
}
