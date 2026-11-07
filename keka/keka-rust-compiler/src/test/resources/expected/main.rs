mod keka_rt;
pub mod keka_samples;

fn main() {
    let vajram = std::env::args().nth(1).unwrap_or_else(|| {
        eprintln!("usage: <program> <vajram-name> [--input value]...");
        std::process::exit(2);
    });
    let mut arguments = std::env::args().skip(2);
    let mut inputs = std::collections::HashMap::new();
    let arena = crate::keka_rt::Arena::new();
    let app_context = crate::keka_rt::AppContext::new(std::rc::Rc::new(crate::keka_rt::DefaultInjector::default()));
    while let Some(flag) = arguments.next() {
        let input = flag.strip_prefix("--").filter(|input| !input.is_empty()).unwrap_or_else(|| {
            eprintln!("expected input flag in the form --input value, got {}", flag);
            std::process::exit(2);
        });
        let value = arguments.next().unwrap_or_else(|| {
            eprintln!("missing value for input {}", input);
            std::process::exit(2);
        });
        if inputs.insert(input.to_owned(), value).is_some() {
            eprintln!("input {} was specified more than once", input);
            std::process::exit(2);
        }
    }
    match vajram.as_str() {
        "a2SqPlusB2Sq" => {
            for input in inputs.keys() {
                if !["a", "b"].contains(&input.as_str()) {
                    eprintln!("unknown input {} for a2SqPlusB2Sq", input);
                    std::process::exit(2);
                }
            }
            let runtime = tokio::runtime::Builder::new_current_thread().enable_all().build().unwrap();
            let local = tokio::task::LocalSet::new();
            local.block_on(&runtime, async {
                let output = keka_samples::calculator::formulas::a2_sq_plus_b2_sq::call(
                    vec![keka_samples::calculator::formulas::a2_sq_plus_b2_sq::A2SqPlusB2SqInputs {
                        a: arena.alloc(
                            inputs
                                .get("a")
                                .unwrap_or_else(|| {
                                    eprintln!("missing input a for a2SqPlusB2Sq");
                                    std::process::exit(2)
                                })
                                .parse::<i32>()
                                .unwrap_or_else(|_| {
                                    eprintln!("invalid numeric input a for a2SqPlusB2Sq");
                                    std::process::exit(2)
                                }),
                        ),
                        b: arena.alloc(
                            inputs
                                .get("b")
                                .unwrap_or_else(|| {
                                    eprintln!("missing input b for a2SqPlusB2Sq");
                                    std::process::exit(2)
                                })
                                .parse::<i32>()
                                .unwrap_or_else(|_| {
                                    eprintln!("invalid numeric input b for a2SqPlusB2Sq");
                                    std::process::exit(2)
                                }),
                        ),
                    }],
                    &arena,
                    &app_context,
                )
                .await
                .into_iter()
                .next()
                .expect("outside-process Vajram result");
                println!("{}", output);
            });
        }
        "activeWeatherAlerts" => {
            for input in inputs.keys() {
                if ![].contains(&input.as_str()) {
                    eprintln!("unknown input {} for activeWeatherAlerts", input);
                    std::process::exit(2);
                }
            }
            let runtime = tokio::runtime::Builder::new_current_thread().enable_all().build().unwrap();
            let local = tokio::task::LocalSet::new();
            local.block_on(&runtime, async {
                let output =
                    keka_samples::weather::weather::active_weather_alerts::call(vec![keka_samples::weather::weather::active_weather_alerts::ActiveWeatherAlertsInputs {}], &arena, &app_context)
                        .await
                        .into_iter()
                        .next()
                        .expect("outside-process Vajram result");
                println!("{}", output);
            });
        }
        "activeWeatherAlertsCount" => {
            for input in inputs.keys() {
                if ![].contains(&input.as_str()) {
                    eprintln!("unknown input {} for activeWeatherAlertsCount", input);
                    std::process::exit(2);
                }
            }
            let runtime = tokio::runtime::Builder::new_current_thread().enable_all().build().unwrap();
            let local = tokio::task::LocalSet::new();
            local.block_on(&runtime, async {
                let output = keka_samples::weather::weather::active_weather_alerts_count::call(
                    vec![keka_samples::weather::weather::active_weather_alerts_count::ActiveWeatherAlertsCountInputs {}],
                    &arena,
                    &app_context,
                )
                .await
                .into_iter()
                .next()
                .expect("outside-process Vajram result");
                println!("{}", output);
            });
        }
        "add" => {
            for input in inputs.keys() {
                if !["a", "b"].contains(&input.as_str()) {
                    eprintln!("unknown input {} for add", input);
                    std::process::exit(2);
                }
            }
            let runtime = tokio::runtime::Builder::new_current_thread().enable_all().build().unwrap();
            let local = tokio::task::LocalSet::new();
            local.block_on(&runtime, async {
                let output = keka_samples::calculator::basic_math_ops::add::call(
                    vec![keka_samples::calculator::basic_math_ops::add::AddInputs {
                        a: arena.alloc(
                            inputs
                                .get("a")
                                .unwrap_or_else(|| {
                                    eprintln!("missing input a for add");
                                    std::process::exit(2)
                                })
                                .parse::<i32>()
                                .unwrap_or_else(|_| {
                                    eprintln!("invalid numeric input a for add");
                                    std::process::exit(2)
                                }),
                        ),
                        b: arena.alloc(
                            inputs
                                .get("b")
                                .unwrap_or_else(|| {
                                    eprintln!("missing input b for add");
                                    std::process::exit(2)
                                })
                                .parse::<i32>()
                                .unwrap_or_else(|_| {
                                    eprintln!("invalid numeric input b for add");
                                    std::process::exit(2)
                                }),
                        ),
                    }],
                    &arena,
                    &app_context,
                )
                .await
                .into_iter()
                .next()
                .expect("outside-process Vajram result");
                println!("{}", output);
            });
        }
        "headFile" => {
            for input in inputs.keys() {
                if !["numChars", "filePath"].contains(&input.as_str()) {
                    eprintln!("unknown input {} for headFile", input);
                    std::process::exit(2);
                }
            }
            let runtime = tokio::runtime::Builder::new_current_thread().enable_all().build().unwrap();
            let local = tokio::task::LocalSet::new();
            local.block_on(&runtime, async {
                let output = keka_samples::hello::head_file::head_file::call(
                    vec![keka_samples::hello::head_file::head_file::HeadFileInputs {
                        numChars: arena.alloc(
                            inputs
                                .get("numChars")
                                .unwrap_or_else(|| {
                                    eprintln!("missing input numChars for headFile");
                                    std::process::exit(2)
                                })
                                .parse::<i32>()
                                .unwrap_or_else(|_| {
                                    eprintln!("invalid numeric input numChars for headFile");
                                    std::process::exit(2)
                                }),
                        ),
                        filePath: arena.alloc(
                            inputs
                                .get("filePath")
                                .unwrap_or_else(|| {
                                    eprintln!("missing input filePath for headFile");
                                    std::process::exit(2)
                                })
                                .to_owned(),
                        ),
                    }],
                    &arena,
                    &app_context,
                )
                .await
                .into_iter()
                .next()
                .expect("outside-process Vajram result");
                println!("{}", output);
            });
        }
        "helloWorld" => {
            for input in inputs.keys() {
                if ![].contains(&input.as_str()) {
                    eprintln!("unknown input {} for helloWorld", input);
                    std::process::exit(2);
                }
            }
            let output = keka_samples::hello::hello_world::hello_world::call(vec![keka_samples::hello::hello_world::hello_world::HelloWorldInputs {}], &arena, &app_context)
                .into_iter()
                .next()
                .expect("outside-process Vajram result");
            let _ = output;
        }
        "helloWorld2" => {
            for input in inputs.keys() {
                if !["name"].contains(&input.as_str()) {
                    eprintln!("unknown input {} for helloWorld2", input);
                    std::process::exit(2);
                }
            }
            let output = keka_samples::hello::hello_world2::hello_world2::call(
                vec![keka_samples::hello::hello_world2::hello_world2::HelloWorld2Inputs {
                    name: arena.alloc(
                        inputs
                            .get("name")
                            .unwrap_or_else(|| {
                                eprintln!("missing input name for helloWorld2");
                                std::process::exit(2)
                            })
                            .to_owned(),
                    ),
                }],
                &arena,
                &app_context,
            )
            .into_iter()
            .next()
            .expect("outside-process Vajram result");
            println!("{}", output);
        }
        "multiHeadFiles" => {
            for input in inputs.keys() {
                if !["separator", "filePath"].contains(&input.as_str()) {
                    eprintln!("unknown input {} for multiHeadFiles", input);
                    std::process::exit(2);
                }
            }
            let runtime = tokio::runtime::Builder::new_current_thread().enable_all().build().unwrap();
            let local = tokio::task::LocalSet::new();
            local.block_on(&runtime, async {
                let output = keka_samples::hello::multi_head_files::multi_head_files::call(
                    vec![keka_samples::hello::multi_head_files::multi_head_files::MultiHeadFilesInputs {
                        separator: arena.alloc(
                            inputs
                                .get("separator")
                                .unwrap_or_else(|| {
                                    eprintln!("missing input separator for multiHeadFiles");
                                    std::process::exit(2)
                                })
                                .to_owned(),
                        ),
                        filePath: arena.alloc(
                            inputs
                                .get("filePath")
                                .unwrap_or_else(|| {
                                    eprintln!("missing input filePath for multiHeadFiles");
                                    std::process::exit(2)
                                })
                                .to_owned(),
                        ),
                    }],
                    &arena,
                    &app_context,
                )
                .await
                .into_iter()
                .next()
                .expect("outside-process Vajram result");
                println!("{}", output);
            });
        }
        "multiply" => {
            for input in inputs.keys() {
                if !["a", "b"].contains(&input.as_str()) {
                    eprintln!("unknown input {} for multiply", input);
                    std::process::exit(2);
                }
            }
            let runtime = tokio::runtime::Builder::new_current_thread().enable_all().build().unwrap();
            let local = tokio::task::LocalSet::new();
            local.block_on(&runtime, async {
                let output = keka_samples::calculator::basic_math_ops::multiply::call(
                    vec![keka_samples::calculator::basic_math_ops::multiply::MultiplyInputs {
                        a: arena.alloc(
                            inputs
                                .get("a")
                                .unwrap_or_else(|| {
                                    eprintln!("missing input a for multiply");
                                    std::process::exit(2)
                                })
                                .parse::<i32>()
                                .unwrap_or_else(|_| {
                                    eprintln!("invalid numeric input a for multiply");
                                    std::process::exit(2)
                                }),
                        ),
                        b: arena.alloc(
                            inputs
                                .get("b")
                                .unwrap_or_else(|| {
                                    eprintln!("missing input b for multiply");
                                    std::process::exit(2)
                                })
                                .parse::<i32>()
                                .unwrap_or_else(|_| {
                                    eprintln!("invalid numeric input b for multiply");
                                    std::process::exit(2)
                                }),
                        ),
                    }],
                    &arena,
                    &app_context,
                )
                .await
                .into_iter()
                .next()
                .expect("outside-process Vajram result");
                println!("{}", output);
            });
        }
        "sayHelloToFriends" => {
            for input in inputs.keys() {
                if !["userId"].contains(&input.as_str()) {
                    eprintln!("unknown input {} for sayHelloToFriends", input);
                    std::process::exit(2);
                }
            }
            let runtime = tokio::runtime::Builder::new_current_thread().enable_all().build().unwrap();
            let local = tokio::task::LocalSet::new();
            local.block_on(&runtime, async {
                let output = keka_samples::friends::say_hello_to_friends::say_hello_to_friends::call(
                    vec![keka_samples::friends::say_hello_to_friends::say_hello_to_friends::SayHelloToFriendsInputs {
                        userId: arena.alloc(
                            inputs
                                .get("userId")
                                .unwrap_or_else(|| {
                                    eprintln!("missing input userId for sayHelloToFriends");
                                    std::process::exit(2)
                                })
                                .to_owned(),
                        ),
                    }],
                    &arena,
                    &app_context,
                )
                .await
                .into_iter()
                .next()
                .expect("outside-process Vajram result");
                println!("{}", output);
            });
        }
        "twoHeadFiles" => {
            for input in inputs.keys() {
                if !["separator", "filePath1", "filePath2"].contains(&input.as_str()) {
                    eprintln!("unknown input {} for twoHeadFiles", input);
                    std::process::exit(2);
                }
            }
            let runtime = tokio::runtime::Builder::new_current_thread().enable_all().build().unwrap();
            let local = tokio::task::LocalSet::new();
            local.block_on(&runtime, async {
                let output = keka_samples::hello::multi_head_files::two_head_files::call(
                    vec![keka_samples::hello::multi_head_files::two_head_files::TwoHeadFilesInputs {
                        separator: arena.alloc(
                            inputs
                                .get("separator")
                                .unwrap_or_else(|| {
                                    eprintln!("missing input separator for twoHeadFiles");
                                    std::process::exit(2)
                                })
                                .to_owned(),
                        ),
                        filePath1: arena.alloc(
                            inputs
                                .get("filePath1")
                                .unwrap_or_else(|| {
                                    eprintln!("missing input filePath1 for twoHeadFiles");
                                    std::process::exit(2)
                                })
                                .to_owned(),
                        ),
                        filePath2: arena.alloc(
                            inputs
                                .get("filePath2")
                                .unwrap_or_else(|| {
                                    eprintln!("missing input filePath2 for twoHeadFiles");
                                    std::process::exit(2)
                                })
                                .to_owned(),
                        ),
                    }],
                    &arena,
                    &app_context,
                )
                .await
                .into_iter()
                .next()
                .expect("outside-process Vajram result");
                println!("{}", output);
            });
        }
        _ => {
            eprintln!("Vajram {} has not been declared to be called from outside-process", vajram);
            std::process::exit(2);
        }
    }
}
