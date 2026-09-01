mod support;

#[test]
fn runs_two_head_files() {
    let first = std::env::temp_dir().join(format!("vajram-two-head-first-{}.txt", std::process::id()));
    let second = std::env::temp_dir().join(format!("vajram-two-head-second-{}.txt", std::process::id()));
    std::fs::write(&first, "hello").expect("first sample file should be written");
    std::fs::write(&second, "cafe").expect("second sample file should be written");
    support::assert_vajram_output(
        "twoHeadFiles",
        &[
            "--separator",
            "|",
            "--filePath1",
            first.to_str().expect("temporary path must be UTF-8"),
            "--filePath2",
            second.to_str().expect("temporary path must be UTF-8"),
        ],
        "hello|cafe",
    );
    let _ = std::fs::remove_file(&first);
    let _ = std::fs::remove_file(&second);
}
