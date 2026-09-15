mod support;

#[test]
fn runs_multi_head_files() {
    let path = std::env::temp_dir().join(format!("vajram-multi-head-{}.txt", std::process::id()));
    std::fs::write(&path, "hello cafe").expect("sample file should be written");
    support::assert_vajram_output(
        "multiHeadFiles",
        &["--separator", "|", "--filePath", path.to_str().expect("temporary path must be UTF-8")],
        "hello cafe|hello cafe",
    );
    let _ = std::fs::remove_file(&path);
}
