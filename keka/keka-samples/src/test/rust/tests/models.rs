use keka_samples::keka_rt::nil;
use keka_samples::keka_samples::models::models::{Account, Money};
use std::collections::HashSet;

fn money(amount: i64, currency: &str) -> Money {
    Money {
        amountMinor: amount,
        currency: currency.to_string(),
    }
}

fn account(id: &str, owner: &str) -> Account {
    Account {
        id: id.to_string(),
        owner: owner.to_string(),
        balance: money(1, "USD"),
        nickname: nil(),
        tier: 1,
        history: vec![],
    }
}

#[test]
fn entities_are_equal_and_hashed_by_id_only() {
    assert_eq!(account("a1", "ram"), account("a1", "someone else"));
    assert_ne!(account("a1", "ram"), account("a2", "ram"));
    let accounts: HashSet<Account> = [
        account("a1", "ram"),
        account("a1", "someone else"),
        account("a2", "ram"),
    ]
    .into_iter()
    .collect();
    assert_eq!(accounts.len(), 2);
}

#[test]
fn values_are_equal_and_hashed_by_every_field() {
    assert_eq!(money(5, "USD"), money(5, "USD"));
    assert_ne!(money(5, "USD"), money(5, "EUR"));
    assert_ne!(money(5, "USD"), money(6, "USD"));
    let values: HashSet<Money> = [money(5, "USD"), money(5, "USD"), money(5, "EUR")]
        .into_iter()
        .collect();
    assert_eq!(values.len(), 2);
}
