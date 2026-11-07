mod support;

#[test]
fn runs_head_file() {
    let path = std::env::temp_dir().join(format!("vajram-head-file-{}.txt", std::process::id()));
    std::fs::write(&path, "hello cafe").expect("test file should be written");
    support::assert_vajram_output(
        "headFile",
        &["--numChars", "5", "--filePath", path.to_str().expect("temporary path must be UTF-8")],
        "hello",
    );
    let _ = std::fs::remove_file(&path);
}
