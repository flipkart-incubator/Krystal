mod keka_rt;
pub mod keka_samples;

fn main() {
    let _vajram = std::env::args().nth(1).unwrap_or_else(|| {
        eprintln!("usage: <program> <vajram-name> [--input value]...");
        std::process::exit(2);
    });
    let mut _arguments = std::env::args().skip(2);
    let mut _inputs: std::collections::HashMap<String, Vec<String>> = std::collections::HashMap::new();
    let _arena = crate::keka_rt::Arena::new();
    let _app_context = crate::keka_rt::AppContext::new(std::rc::Rc::new(crate::keka_rt::DefaultInjector::default()));
    while let Some(_flag) = _arguments.next() {
        let _input = _flag.strip_prefix("--").filter(|_input| !_input.is_empty()).unwrap_or_else(|| {
            eprintln!("expected input flag in the form --input value, got {}", _flag);
            std::process::exit(2);
        });
        let _value = _arguments.next().unwrap_or_else(|| {
            eprintln!("missing value for input {}", _input);
            std::process::exit(2);
        });
        _inputs.entry(_input.to_owned()).or_default().push(_value);
    }
    match _vajram.as_str() {
        "a2SqPlusB2Sq" => {
            for _input in _inputs.keys() {
                if !["a", "b"].contains(&_input.as_str()) {
                    eprintln!("unknown input {} for a2SqPlusB2Sq", _input);
                    std::process::exit(2);
                }
            }
            let _runtime = tokio::runtime::Builder::new_current_thread().enable_all().build().unwrap();
            let _local = tokio::task::LocalSet::new();
            _local.block_on(&_runtime, async {
                let _output = keka_samples::calculator::formulas::a2_sq_plus_b2_sq::call(
                    vec![keka_samples::calculator::formulas::a2_sq_plus_b2_sq::A2SqPlusB2SqInputs {
                        a: _arena.alloc(
                            _inputs
                                .get("a")
                                .map(|_values| {
                                    if _values.len() > 1 {
                                        eprintln!("input a was specified more than once");
                                        std::process::exit(2)
                                    }
                                    &_values[0]
                                })
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
                        b: _arena.alloc(
                            _inputs
                                .get("b")
                                .map(|_values| {
                                    if _values.len() > 1 {
                                        eprintln!("input b was specified more than once");
                                        std::process::exit(2)
                                    }
                                    &_values[0]
                                })
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
                    &_arena,
                    &_app_context,
                )
                .await
                .into_iter()
                .next()
                .expect("outside-process Vajram result");
                let _output = match _output {
                    Ok(_value) => _value,
                    Err(_error) => {
                        eprintln!("error: {}", _error);
                        std::process::exit(1)
                    }
                };
                println!("{}", _output);
            });
        }
        "activeWeatherAlerts" => {
            for _input in _inputs.keys() {
                if ![].contains(&_input.as_str()) {
                    eprintln!("unknown input {} for activeWeatherAlerts", _input);
                    std::process::exit(2);
                }
            }
            let _runtime = tokio::runtime::Builder::new_current_thread().enable_all().build().unwrap();
            let _local = tokio::task::LocalSet::new();
            _local.block_on(&_runtime, async {
                let _output =
                    keka_samples::weather::weather::active_weather_alerts::call(vec![keka_samples::weather::weather::active_weather_alerts::ActiveWeatherAlertsInputs {}], &_arena, &_app_context)
                        .await
                        .into_iter()
                        .next()
                        .expect("outside-process Vajram result");
                let _output = match _output {
                    Ok(_value) => _value,
                    Err(_error) => {
                        eprintln!("error: {}", _error);
                        std::process::exit(1)
                    }
                };
                println!("{}", _output);
            });
        }
        "activeWeatherAlertsCount" => {
            for _input in _inputs.keys() {
                if ![].contains(&_input.as_str()) {
                    eprintln!("unknown input {} for activeWeatherAlertsCount", _input);
                    std::process::exit(2);
                }
            }
            let _runtime = tokio::runtime::Builder::new_current_thread().enable_all().build().unwrap();
            let _local = tokio::task::LocalSet::new();
            _local.block_on(&_runtime, async {
                let _output = keka_samples::weather::weather::active_weather_alerts_count::call(
                    vec![keka_samples::weather::weather::active_weather_alerts_count::ActiveWeatherAlertsCountInputs {}],
                    &_arena,
                    &_app_context,
                )
                .await
                .into_iter()
                .next()
                .expect("outside-process Vajram result");
                let _output = match _output {
                    Ok(_value) => _value,
                    Err(_error) => {
                        eprintln!("error: {}", _error);
                        std::process::exit(1)
                    }
                };
                println!("{}", _output);
            });
        }
        "add" => {
            for _input in _inputs.keys() {
                if !["a", "b"].contains(&_input.as_str()) {
                    eprintln!("unknown input {} for add", _input);
                    std::process::exit(2);
                }
            }
            let _runtime = tokio::runtime::Builder::new_current_thread().enable_all().build().unwrap();
            let _local = tokio::task::LocalSet::new();
            _local.block_on(&_runtime, async {
                let _output = keka_samples::calculator::basic_math_ops::add::call(
                    vec![keka_samples::calculator::basic_math_ops::add::AddInputs {
                        a: _arena.alloc(
                            _inputs
                                .get("a")
                                .map(|_values| {
                                    if _values.len() > 1 {
                                        eprintln!("input a was specified more than once");
                                        std::process::exit(2)
                                    }
                                    &_values[0]
                                })
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
                        b: _arena.alloc(
                            _inputs
                                .get("b")
                                .map(|_values| {
                                    if _values.len() > 1 {
                                        eprintln!("input b was specified more than once");
                                        std::process::exit(2)
                                    }
                                    &_values[0]
                                })
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
                    &_arena,
                    &_app_context,
                )
                .await
                .into_iter()
                .next()
                .expect("outside-process Vajram result");
                let _output = match _output {
                    Ok(_value) => _value,
                    Err(_error) => {
                        eprintln!("error: {}", _error);
                        std::process::exit(1)
                    }
                };
                println!("{}", _output);
            });
        }
        "checkCallPairing" => {
            for _input in _inputs.keys() {
                if !["path"].contains(&_input.as_str()) {
                    eprintln!("unknown input {} for checkCallPairing", _input);
                    std::process::exit(2);
                }
            }
            let _runtime = tokio::runtime::Builder::new_current_thread().enable_all().build().unwrap();
            let _local = tokio::task::LocalSet::new();
            _local.block_on(&_runtime, async {
                let _output = keka_samples::errors::error_handling::check_call_pairing::call(
                    vec![keka_samples::errors::error_handling::check_call_pairing::CheckCallPairingInputs {
                        path: _arena.alloc(
                            _inputs
                                .get("path")
                                .map(|_values| {
                                    if _values.len() > 1 {
                                        eprintln!("input path was specified more than once");
                                        std::process::exit(2)
                                    }
                                    &_values[0]
                                })
                                .unwrap_or_else(|| {
                                    eprintln!("missing input path for checkCallPairing");
                                    std::process::exit(2)
                                })
                                .to_owned(),
                        ),
                    }],
                    &_arena,
                    &_app_context,
                )
                .await
                .into_iter()
                .next()
                .expect("outside-process Vajram result");
                let _output = match _output {
                    Ok(_value) => _value,
                    Err(_error) => {
                        eprintln!("error: {}", _error);
                        std::process::exit(1)
                    }
                };
                println!("{}", _output);
            });
        }
        "checkErrors" => {
            for _input in _inputs.keys() {
                if !["input"].contains(&_input.as_str()) {
                    eprintln!("unknown input {} for checkErrors", _input);
                    std::process::exit(2);
                }
            }
            let _runtime = tokio::runtime::Builder::new_current_thread().enable_all().build().unwrap();
            let _local = tokio::task::LocalSet::new();
            _local.block_on(&_runtime, async {
                let _output = keka_samples::errors::error_handling::check_errors::call(
                    vec![keka_samples::errors::error_handling::check_errors::CheckErrorsInputs {
                        input: _arena.alloc(
                            _inputs
                                .get("input")
                                .map(|_values| {
                                    if _values.len() > 1 {
                                        eprintln!("input input was specified more than once");
                                        std::process::exit(2)
                                    }
                                    &_values[0]
                                })
                                .unwrap_or_else(|| {
                                    eprintln!("missing input input for checkErrors");
                                    std::process::exit(2)
                                })
                                .to_owned(),
                        ),
                    }],
                    &_arena,
                    &_app_context,
                )
                .await
                .into_iter()
                .next()
                .expect("outside-process Vajram result");
                let _output = match _output {
                    Ok(_value) => _value,
                    Err(_error) => {
                        eprintln!("error: {}", _error);
                        std::process::exit(1)
                    }
                };
                println!("{}", _output);
            });
        }
        "checkErrorsInFanout" => {
            for _input in _inputs.keys() {
                if !["input"].contains(&_input.as_str()) {
                    eprintln!("unknown input {} for checkErrorsInFanout", _input);
                    std::process::exit(2);
                }
            }
            let _runtime = tokio::runtime::Builder::new_current_thread().enable_all().build().unwrap();
            let _local = tokio::task::LocalSet::new();
            _local.block_on(&_runtime, async {
                let _output = keka_samples::errors::error_handling::check_errors_in_fanout::call(
                    vec![keka_samples::errors::error_handling::check_errors_in_fanout::CheckErrorsInFanoutInputs {
                        input: _arena.alloc(
                            _inputs
                                .get("input")
                                .map(|_values| {
                                    if _values.len() > 1 {
                                        eprintln!("input input was specified more than once");
                                        std::process::exit(2)
                                    }
                                    &_values[0]
                                })
                                .unwrap_or_else(|| {
                                    eprintln!("missing input input for checkErrorsInFanout");
                                    std::process::exit(2)
                                })
                                .to_owned(),
                        ),
                    }],
                    &_arena,
                    &_app_context,
                )
                .await
                .into_iter()
                .next()
                .expect("outside-process Vajram result");
                let _output = match _output {
                    Ok(_value) => _value,
                    Err(_error) => {
                        eprintln!("error: {}", _error);
                        std::process::exit(1)
                    }
                };
                println!("{}", _output);
            });
        }
        "checkPartialFailure" => {
            for _input in _inputs.keys() {
                if !["path"].contains(&_input.as_str()) {
                    eprintln!("unknown input {} for checkPartialFailure", _input);
                    std::process::exit(2);
                }
            }
            let _runtime = tokio::runtime::Builder::new_current_thread().enable_all().build().unwrap();
            let _local = tokio::task::LocalSet::new();
            _local.block_on(&_runtime, async {
                let _output = keka_samples::errors::error_handling::check_partial_failure::call(
                    vec![keka_samples::errors::error_handling::check_partial_failure::CheckPartialFailureInputs {
                        path: _arena.alloc(
                            _inputs
                                .get("path")
                                .map(|_values| {
                                    if _values.len() > 1 {
                                        eprintln!("input path was specified more than once");
                                        std::process::exit(2)
                                    }
                                    &_values[0]
                                })
                                .unwrap_or_else(|| {
                                    eprintln!("missing input path for checkPartialFailure");
                                    std::process::exit(2)
                                })
                                .to_owned(),
                        ),
                    }],
                    &_arena,
                    &_app_context,
                )
                .await
                .into_iter()
                .next()
                .expect("outside-process Vajram result");
                let _output = match _output {
                    Ok(_value) => _value,
                    Err(_error) => {
                        eprintln!("error: {}", _error);
                        std::process::exit(1)
                    }
                };
                println!("{}", _output);
            });
        }
        "checkSingleCall" => {
            for _input in _inputs.keys() {
                if !["input"].contains(&_input.as_str()) {
                    eprintln!("unknown input {} for checkSingleCall", _input);
                    std::process::exit(2);
                }
            }
            let _runtime = tokio::runtime::Builder::new_current_thread().enable_all().build().unwrap();
            let _local = tokio::task::LocalSet::new();
            _local.block_on(&_runtime, async {
                let _output = keka_samples::errors::error_handling::check_single_call::call(
                    vec![keka_samples::errors::error_handling::check_single_call::CheckSingleCallInputs {
                        input: _arena.alloc(
                            _inputs
                                .get("input")
                                .map(|_values| {
                                    if _values.len() > 1 {
                                        eprintln!("input input was specified more than once");
                                        std::process::exit(2)
                                    }
                                    &_values[0]
                                })
                                .unwrap_or_else(|| {
                                    eprintln!("missing input input for checkSingleCall");
                                    std::process::exit(2)
                                })
                                .to_owned(),
                        ),
                    }],
                    &_arena,
                    &_app_context,
                )
                .await
                .into_iter()
                .next()
                .expect("outside-process Vajram result");
                let _output = match _output {
                    Ok(_value) => _value,
                    Err(_error) => {
                        eprintln!("error: {}", _error);
                        std::process::exit(1)
                    }
                };
                println!("{}", _output);
            });
        }
        "describeAccount" => {
            for _input in _inputs.keys() {
                if !["id"].contains(&_input.as_str()) {
                    eprintln!("unknown input {} for describeAccount", _input);
                    std::process::exit(2);
                }
            }
            let _runtime = tokio::runtime::Builder::new_current_thread().enable_all().build().unwrap();
            let _local = tokio::task::LocalSet::new();
            _local.block_on(&_runtime, async {
                let _output = keka_samples::models::accounts::describe_account::call(
                    vec![keka_samples::models::accounts::describe_account::DescribeAccountInputs {
                        id: _arena.alloc(
                            _inputs
                                .get("id")
                                .map(|_values| {
                                    if _values.len() > 1 {
                                        eprintln!("input id was specified more than once");
                                        std::process::exit(2)
                                    }
                                    &_values[0]
                                })
                                .unwrap_or_else(|| {
                                    eprintln!("missing input id for describeAccount");
                                    std::process::exit(2)
                                })
                                .to_owned(),
                        ),
                    }],
                    &_arena,
                    &_app_context,
                )
                .await
                .into_iter()
                .next()
                .expect("outside-process Vajram result");
                let _output = match _output {
                    Ok(_value) => _value,
                    Err(_error) => {
                        eprintln!("error: {}", _error);
                        std::process::exit(1)
                    }
                };
                println!("{}", _output);
            });
        }
        "differentMoney" => {
            for _input in _inputs.keys() {
                if !["amountMinor"].contains(&_input.as_str()) {
                    eprintln!("unknown input {} for differentMoney", _input);
                    std::process::exit(2);
                }
            }
            let _runtime = tokio::runtime::Builder::new_current_thread().enable_all().build().unwrap();
            let _local = tokio::task::LocalSet::new();
            _local.block_on(&_runtime, async {
                let _output = keka_samples::models::accounts::different_money::call(
                    vec![keka_samples::models::accounts::different_money::DifferentMoneyInputs {
                        amountMinor: _arena.alloc(
                            _inputs
                                .get("amountMinor")
                                .map(|_values| {
                                    if _values.len() > 1 {
                                        eprintln!("input amountMinor was specified more than once");
                                        std::process::exit(2)
                                    }
                                    &_values[0]
                                })
                                .unwrap_or_else(|| {
                                    eprintln!("missing input amountMinor for differentMoney");
                                    std::process::exit(2)
                                })
                                .parse::<i64>()
                                .unwrap_or_else(|_| {
                                    eprintln!("invalid numeric input amountMinor for differentMoney");
                                    std::process::exit(2)
                                }),
                        ),
                    }],
                    &_arena,
                    &_app_context,
                )
                .await
                .into_iter()
                .next()
                .expect("outside-process Vajram result");
                let _output = match _output {
                    Ok(_value) => _value,
                    Err(_error) => {
                        eprintln!("error: {}", _error);
                        std::process::exit(1)
                    }
                };
                println!("{}", _output);
            });
        }
        "headFile" => {
            for _input in _inputs.keys() {
                if !["numChars", "filePath"].contains(&_input.as_str()) {
                    eprintln!("unknown input {} for headFile", _input);
                    std::process::exit(2);
                }
            }
            let _runtime = tokio::runtime::Builder::new_current_thread().enable_all().build().unwrap();
            let _local = tokio::task::LocalSet::new();
            _local.block_on(&_runtime, async {
                let _output = keka_samples::hello::head_file::head_file::call(
                    vec![keka_samples::hello::head_file::head_file::HeadFileInputs {
                        numChars: _arena.alloc(
                            _inputs
                                .get("numChars")
                                .map(|_values| {
                                    if _values.len() > 1 {
                                        eprintln!("input numChars was specified more than once");
                                        std::process::exit(2)
                                    }
                                    &_values[0]
                                })
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
                        filePath: _arena.alloc(
                            _inputs
                                .get("filePath")
                                .map(|_values| {
                                    if _values.len() > 1 {
                                        eprintln!("input filePath was specified more than once");
                                        std::process::exit(2)
                                    }
                                    &_values[0]
                                })
                                .unwrap_or_else(|| {
                                    eprintln!("missing input filePath for headFile");
                                    std::process::exit(2)
                                })
                                .to_owned(),
                        ),
                    }],
                    &_arena,
                    &_app_context,
                )
                .await
                .into_iter()
                .next()
                .expect("outside-process Vajram result");
                let _output = match _output {
                    Ok(_value) => _value,
                    Err(_error) => {
                        eprintln!("error: {}", _error);
                        std::process::exit(1)
                    }
                };
                println!("{}", _output);
            });
        }
        "helloWorld" => {
            for _input in _inputs.keys() {
                if ![].contains(&_input.as_str()) {
                    eprintln!("unknown input {} for helloWorld", _input);
                    std::process::exit(2);
                }
            }
            let _output = keka_samples::hello::hello_world::hello_world::call(vec![keka_samples::hello::hello_world::hello_world::HelloWorldInputs {}], &_arena, &_app_context)
                .into_iter()
                .next()
                .expect("outside-process Vajram result");
            let _ = _output;
        }
        "helloWorld2" => {
            for _input in _inputs.keys() {
                if !["name"].contains(&_input.as_str()) {
                    eprintln!("unknown input {} for helloWorld2", _input);
                    std::process::exit(2);
                }
            }
            let _output = keka_samples::hello::hello_world2::hello_world2::call(
                vec![keka_samples::hello::hello_world2::hello_world2::HelloWorld2Inputs {
                    name: _arena.alloc(
                        _inputs
                            .get("name")
                            .map(|_values| {
                                if _values.len() > 1 {
                                    eprintln!("input name was specified more than once");
                                    std::process::exit(2)
                                }
                                &_values[0]
                            })
                            .unwrap_or_else(|| {
                                eprintln!("missing input name for helloWorld2");
                                std::process::exit(2)
                            })
                            .to_owned(),
                    ),
                }],
                &_arena,
                &_app_context,
            )
            .into_iter()
            .next()
            .expect("outside-process Vajram result");
            println!("{}", _output);
        }
        "multiHeadFiles" => {
            for _input in _inputs.keys() {
                if !["separator", "filePath"].contains(&_input.as_str()) {
                    eprintln!("unknown input {} for multiHeadFiles", _input);
                    std::process::exit(2);
                }
            }
            let _runtime = tokio::runtime::Builder::new_current_thread().enable_all().build().unwrap();
            let _local = tokio::task::LocalSet::new();
            _local.block_on(&_runtime, async {
                let _output = keka_samples::hello::multi_head_files::multi_head_files::call(
                    vec![keka_samples::hello::multi_head_files::multi_head_files::MultiHeadFilesInputs {
                        separator: _arena.alloc(
                            _inputs
                                .get("separator")
                                .map(|_values| {
                                    if _values.len() > 1 {
                                        eprintln!("input separator was specified more than once");
                                        std::process::exit(2)
                                    }
                                    &_values[0]
                                })
                                .unwrap_or_else(|| {
                                    eprintln!("missing input separator for multiHeadFiles");
                                    std::process::exit(2)
                                })
                                .to_owned(),
                        ),
                        filePath: _arena.alloc(
                            _inputs
                                .get("filePath")
                                .map(|_values| {
                                    if _values.len() > 1 {
                                        eprintln!("input filePath was specified more than once");
                                        std::process::exit(2)
                                    }
                                    &_values[0]
                                })
                                .unwrap_or_else(|| {
                                    eprintln!("missing input filePath for multiHeadFiles");
                                    std::process::exit(2)
                                })
                                .to_owned(),
                        ),
                    }],
                    &_arena,
                    &_app_context,
                )
                .await
                .into_iter()
                .next()
                .expect("outside-process Vajram result");
                let _output = match _output {
                    Ok(_value) => _value,
                    Err(_error) => {
                        eprintln!("error: {}", _error);
                        std::process::exit(1)
                    }
                };
                println!("{}", _output);
            });
        }
        "multiply" => {
            for _input in _inputs.keys() {
                if !["a", "b"].contains(&_input.as_str()) {
                    eprintln!("unknown input {} for multiply", _input);
                    std::process::exit(2);
                }
            }
            let _runtime = tokio::runtime::Builder::new_current_thread().enable_all().build().unwrap();
            let _local = tokio::task::LocalSet::new();
            _local.block_on(&_runtime, async {
                let _output = keka_samples::calculator::basic_math_ops::multiply::call(
                    vec![keka_samples::calculator::basic_math_ops::multiply::MultiplyInputs {
                        a: _arena.alloc(
                            _inputs
                                .get("a")
                                .map(|_values| {
                                    if _values.len() > 1 {
                                        eprintln!("input a was specified more than once");
                                        std::process::exit(2)
                                    }
                                    &_values[0]
                                })
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
                        b: _arena.alloc(
                            _inputs
                                .get("b")
                                .map(|_values| {
                                    if _values.len() > 1 {
                                        eprintln!("input b was specified more than once");
                                        std::process::exit(2)
                                    }
                                    &_values[0]
                                })
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
                    &_arena,
                    &_app_context,
                )
                .await
                .into_iter()
                .next()
                .expect("outside-process Vajram result");
                let _output = match _output {
                    Ok(_value) => _value,
                    Err(_error) => {
                        eprintln!("error: {}", _error);
                        std::process::exit(1)
                    }
                };
                println!("{}", _output);
            });
        }
        "nicknameIsNil" => {
            for _input in _inputs.keys() {
                if !["id"].contains(&_input.as_str()) {
                    eprintln!("unknown input {} for nicknameIsNil", _input);
                    std::process::exit(2);
                }
            }
            let _runtime = tokio::runtime::Builder::new_current_thread().enable_all().build().unwrap();
            let _local = tokio::task::LocalSet::new();
            _local.block_on(&_runtime, async {
                let _output = keka_samples::models::accounts::nickname_is_nil::call(
                    vec![keka_samples::models::accounts::nickname_is_nil::NicknameIsNilInputs {
                        id: _arena.alloc(
                            _inputs
                                .get("id")
                                .map(|_values| {
                                    if _values.len() > 1 {
                                        eprintln!("input id was specified more than once");
                                        std::process::exit(2)
                                    }
                                    &_values[0]
                                })
                                .unwrap_or_else(|| {
                                    eprintln!("missing input id for nicknameIsNil");
                                    std::process::exit(2)
                                })
                                .to_owned(),
                        ),
                    }],
                    &_arena,
                    &_app_context,
                )
                .await
                .into_iter()
                .next()
                .expect("outside-process Vajram result");
                let _output = match _output {
                    Ok(_value) => _value,
                    Err(_error) => {
                        eprintln!("error: {}", _error);
                        std::process::exit(1)
                    }
                };
                println!("{}", _output);
            });
        }
        "nicknameIsSet" => {
            for _input in _inputs.keys() {
                if !["id"].contains(&_input.as_str()) {
                    eprintln!("unknown input {} for nicknameIsSet", _input);
                    std::process::exit(2);
                }
            }
            let _runtime = tokio::runtime::Builder::new_current_thread().enable_all().build().unwrap();
            let _local = tokio::task::LocalSet::new();
            _local.block_on(&_runtime, async {
                let _output = keka_samples::models::accounts::nickname_is_set::call(
                    vec![keka_samples::models::accounts::nickname_is_set::NicknameIsSetInputs {
                        id: _arena.alloc(
                            _inputs
                                .get("id")
                                .map(|_values| {
                                    if _values.len() > 1 {
                                        eprintln!("input id was specified more than once");
                                        std::process::exit(2)
                                    }
                                    &_values[0]
                                })
                                .unwrap_or_else(|| {
                                    eprintln!("missing input id for nicknameIsSet");
                                    std::process::exit(2)
                                })
                                .to_owned(),
                        ),
                    }],
                    &_arena,
                    &_app_context,
                )
                .await
                .into_iter()
                .next()
                .expect("outside-process Vajram result");
                let _output = match _output {
                    Ok(_value) => _value,
                    Err(_error) => {
                        eprintln!("error: {}", _error);
                        std::process::exit(1)
                    }
                };
                println!("{}", _output);
            });
        }
        "readAvailable" => {
            for _input in _inputs.keys() {
                if !["path"].contains(&_input.as_str()) {
                    eprintln!("unknown input {} for readAvailable", _input);
                    std::process::exit(2);
                }
            }
            let _runtime = tokio::runtime::Builder::new_current_thread().enable_all().build().unwrap();
            let _local = tokio::task::LocalSet::new();
            _local.block_on(&_runtime, async {
                let _output = keka_samples::errors::error_handling::read_available::call(
                    vec![keka_samples::errors::error_handling::read_available::ReadAvailableInputs {
                        path: _arena.alloc(
                            _inputs
                                .get("path")
                                .map(|_values| {
                                    if _values.len() > 1 {
                                        eprintln!("input path was specified more than once");
                                        std::process::exit(2)
                                    }
                                    &_values[0]
                                })
                                .unwrap_or_else(|| {
                                    eprintln!("missing input path for readAvailable");
                                    std::process::exit(2)
                                })
                                .to_owned(),
                        ),
                    }],
                    &_arena,
                    &_app_context,
                )
                .await
                .into_iter()
                .next()
                .expect("outside-process Vajram result");
                let _output = match _output {
                    Ok(_value) => _value,
                    Err(_error) => {
                        eprintln!("error: {}", _error);
                        std::process::exit(1)
                    }
                };
                println!("{}", _output);
            });
        }
        "sameAccount" => {
            for _input in _inputs.keys() {
                if !["id"].contains(&_input.as_str()) {
                    eprintln!("unknown input {} for sameAccount", _input);
                    std::process::exit(2);
                }
            }
            let _runtime = tokio::runtime::Builder::new_current_thread().enable_all().build().unwrap();
            let _local = tokio::task::LocalSet::new();
            _local.block_on(&_runtime, async {
                let _output = keka_samples::models::accounts::same_account::call(
                    vec![keka_samples::models::accounts::same_account::SameAccountInputs {
                        id: _arena.alloc(
                            _inputs
                                .get("id")
                                .map(|_values| {
                                    if _values.len() > 1 {
                                        eprintln!("input id was specified more than once");
                                        std::process::exit(2)
                                    }
                                    &_values[0]
                                })
                                .unwrap_or_else(|| {
                                    eprintln!("missing input id for sameAccount");
                                    std::process::exit(2)
                                })
                                .to_owned(),
                        ),
                    }],
                    &_arena,
                    &_app_context,
                )
                .await
                .into_iter()
                .next()
                .expect("outside-process Vajram result");
                let _output = match _output {
                    Ok(_value) => _value,
                    Err(_error) => {
                        eprintln!("error: {}", _error);
                        std::process::exit(1)
                    }
                };
                println!("{}", _output);
            });
        }
        "sameMoney" => {
            for _input in _inputs.keys() {
                if !["amountMinor"].contains(&_input.as_str()) {
                    eprintln!("unknown input {} for sameMoney", _input);
                    std::process::exit(2);
                }
            }
            let _runtime = tokio::runtime::Builder::new_current_thread().enable_all().build().unwrap();
            let _local = tokio::task::LocalSet::new();
            _local.block_on(&_runtime, async {
                let _output = keka_samples::models::accounts::same_money::call(
                    vec![keka_samples::models::accounts::same_money::SameMoneyInputs {
                        amountMinor: _arena.alloc(
                            _inputs
                                .get("amountMinor")
                                .map(|_values| {
                                    if _values.len() > 1 {
                                        eprintln!("input amountMinor was specified more than once");
                                        std::process::exit(2)
                                    }
                                    &_values[0]
                                })
                                .unwrap_or_else(|| {
                                    eprintln!("missing input amountMinor for sameMoney");
                                    std::process::exit(2)
                                })
                                .parse::<i64>()
                                .unwrap_or_else(|_| {
                                    eprintln!("invalid numeric input amountMinor for sameMoney");
                                    std::process::exit(2)
                                }),
                        ),
                    }],
                    &_arena,
                    &_app_context,
                )
                .await
                .into_iter()
                .next()
                .expect("outside-process Vajram result");
                let _output = match _output {
                    Ok(_value) => _value,
                    Err(_error) => {
                        eprintln!("error: {}", _error);
                        std::process::exit(1)
                    }
                };
                println!("{}", _output);
            });
        }
        "sayHelloToFriends" => {
            for _input in _inputs.keys() {
                if !["userId"].contains(&_input.as_str()) {
                    eprintln!("unknown input {} for sayHelloToFriends", _input);
                    std::process::exit(2);
                }
            }
            let _runtime = tokio::runtime::Builder::new_current_thread().enable_all().build().unwrap();
            let _local = tokio::task::LocalSet::new();
            _local.block_on(&_runtime, async {
                let _output = keka_samples::friends::say_hello_to_friends::say_hello_to_friends::call(
                    vec![keka_samples::friends::say_hello_to_friends::say_hello_to_friends::SayHelloToFriendsInputs {
                        userId: _arena.alloc(
                            _inputs
                                .get("userId")
                                .map(|_values| {
                                    if _values.len() > 1 {
                                        eprintln!("input userId was specified more than once");
                                        std::process::exit(2)
                                    }
                                    &_values[0]
                                })
                                .unwrap_or_else(|| {
                                    eprintln!("missing input userId for sayHelloToFriends");
                                    std::process::exit(2)
                                })
                                .to_owned(),
                        ),
                    }],
                    &_arena,
                    &_app_context,
                )
                .await
                .into_iter()
                .next()
                .expect("outside-process Vajram result");
                let _output = match _output {
                    Ok(_value) => _value,
                    Err(_error) => {
                        eprintln!("error: {}", _error);
                        std::process::exit(1)
                    }
                };
                println!("{}", _output);
            });
        }
        "twoHeadFiles" => {
            for _input in _inputs.keys() {
                if !["separator", "filePath1", "filePath2"].contains(&_input.as_str()) {
                    eprintln!("unknown input {} for twoHeadFiles", _input);
                    std::process::exit(2);
                }
            }
            let _runtime = tokio::runtime::Builder::new_current_thread().enable_all().build().unwrap();
            let _local = tokio::task::LocalSet::new();
            _local.block_on(&_runtime, async {
                let _output = keka_samples::hello::multi_head_files::two_head_files::call(
                    vec![keka_samples::hello::multi_head_files::two_head_files::TwoHeadFilesInputs {
                        separator: _arena.alloc(
                            _inputs
                                .get("separator")
                                .map(|_values| {
                                    if _values.len() > 1 {
                                        eprintln!("input separator was specified more than once");
                                        std::process::exit(2)
                                    }
                                    &_values[0]
                                })
                                .unwrap_or_else(|| {
                                    eprintln!("missing input separator for twoHeadFiles");
                                    std::process::exit(2)
                                })
                                .to_owned(),
                        ),
                        filePath1: _arena.alloc(
                            _inputs
                                .get("filePath1")
                                .map(|_values| {
                                    if _values.len() > 1 {
                                        eprintln!("input filePath1 was specified more than once");
                                        std::process::exit(2)
                                    }
                                    &_values[0]
                                })
                                .unwrap_or_else(|| {
                                    eprintln!("missing input filePath1 for twoHeadFiles");
                                    std::process::exit(2)
                                })
                                .to_owned(),
                        ),
                        filePath2: _arena.alloc(
                            _inputs
                                .get("filePath2")
                                .map(|_values| {
                                    if _values.len() > 1 {
                                        eprintln!("input filePath2 was specified more than once");
                                        std::process::exit(2)
                                    }
                                    &_values[0]
                                })
                                .unwrap_or_else(|| {
                                    eprintln!("missing input filePath2 for twoHeadFiles");
                                    std::process::exit(2)
                                })
                                .to_owned(),
                        ),
                    }],
                    &_arena,
                    &_app_context,
                )
                .await
                .into_iter()
                .next()
                .expect("outside-process Vajram result");
                let _output = match _output {
                    Ok(_value) => _value,
                    Err(_error) => {
                        eprintln!("error: {}", _error);
                        std::process::exit(1)
                    }
                };
                println!("{}", _output);
            });
        }
        _ => {
            eprintln!("Vajram {} has not been declared to be called from outside-process", _vajram);
            std::process::exit(2);
        }
    }
}
