mod support;

#[test]
fn runs_hello_world_2() {
    support::assert_vajram_output("helloWorld2", &[], "Hello again from vajram-lang!");
}
