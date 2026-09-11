use std::process::Command;

pub fn assert_vajram_output(vajram: &str, arguments: &[&str], expected: &str) {
    let output = Command::new(env!("CARGO_BIN_EXE_vajram-lang-samples"))
        .arg(vajram)
        .args(arguments)
        .output()
        .expect("outsideProcess CLI should start");
    assert!(
        output.status.success(),
        "outsideProcess CLI failed: {}",
        String::from_utf8_lossy(&output.stderr)
    );
    assert_eq!(String::from_utf8_lossy(&output.stdout).trim(), expected);
}
