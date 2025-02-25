1. **Vajram names are verbs in [upper-camel-case](https://en.wikipedia.org/wiki/Camel_case) verbs**: A Vajram is the smallest unit of business logic in the Krystal framework, which performs a particular function. The name of a vajramDef should reflect that functionality.
    1. Vajram names must be verbs: If the vajramDef retrieves and returns some data, then it can be name `GetX`
    2. If the vajramDef updates some data, it can be named `UpdateY`
    3. Other possibilities are `GetAndUpdateZ` , `LoginUser` , `SendSMS` etc.
2. **Vajram Ids are purely functional in nature**:
    1. **Avoid common suffixes** Internally krystal reads all vajramDef definitions and creates a Directed Acyclic Graph of kryons (Each kryon corresponds to exactly one vajramDef and is a node in the graph). So for someone who understands the internals of Krystal, it might be tempting to name the vajramDef with a 'Node', 'Vajram' or 'Kryon' in the vajramDef id. This is not advisable. The name of the vajramDef should only refer to what it does and nothing else.
    2. **Don't use `Batch` in vajramDef name** Some vajrams can have input batching enabled (for example, batching), but krystal abstracts out this implementation detail to this vajramDef's client vajrams. This means a vajramDef which has input batching disabled now may enable it a later point in time, or vice versa. This is the reason the word `Batch` should not be present in the vajramDef Id.
3. **Try to keep vajramDef names short**: The vajramDef SDK uses code generation extensively to make developers' life easy by making data access type-safe. The id of the vajramDef is used as a prefix of all generetated classes related to the vajramDef. So a long vajramDef name will lead to even longer class names. For example, if a vajramDef's name is 'getAndUpdateUserNameFromUserService`, krystal generates the following classes:
    1. `public class GetAndUpdateUserNameFromUserServiceRequest`
    2. If Vajram has input batching disabled:
        1. package-private `class GetAndUpdateUserNameFromUserServiceFacets`
    3. If vajramDef has input batching enabled
        1. package-private `GetAndUpdateUserNameFromUserServiceModInputs`
        2. package-private `GetAndUpdateUserNameFromUserServiceCommonFacets`
        3. `public class GetAndUpdateUserNameFromUserServiceImpl`
4. **Vajram names are globally unique**: Unlike class names which areunique within a package, vajramDef names are supposed to be globally unique. This allows vajrams to be reusbale across various services and deployments. So name the vajramDef accordingly. This also means that refactoring vajrams by moving the java file to a different package, module or repo does not impact the vajramDef Id.