# How to write a vajram
1. First, decide on the functional scope of the Vajram. What is the core functionality of the vajram, or what does it do?
2. Based on the above, decide on a name based on the these Vajram naming conventions:
   1. **Vajram names are verbs in [upper-camel-case](https://en.wikipedia.org/wiki/Camel_case) verbs**: A Vajram is the smallest unit of business logic in the Krystal framework, which performs a particular function. The name of a vajram should reflect that functionality.
      1. Vajram names must be verbs: If the vajram retrieves and returns some data, then it can be name `GetX`
      2. If the vajram updates some data, it can be named `UpdateY`
      3. Other possibilities are `GetAndUpdateZ` , `LoginUser` , `SendSMS`  etc.
   2. **Vajram Ids are purely functional in nature**:
      1. **Avoid common suffixes** Internally krystal reads all vajram definitions and creates a Directed Acyclic Graph of kryons (Each kryon corresponds to exactly one vajram and is a node in the graph). So for someone who understands the internals of Krystal, it might be tempting to name the vajram with a 'Node', 'Vajram' or 'Kryon' in the vajram id. This is not advisable. The name of the vajram should only refer to what it does and nothing else.
      2. **Don't use `Batch` in vajram name** Some vajrams can have input modulation enabled (for example, batching), but krystal abstracts out this implementation detail to this vajram's client vajrams. This means a vajram which has input modulation disabled now may enable it a later point in time, or vice versa. This is the reason the word `Batch` should not be present in the vajram Id.
   3. **Try to keep vajram names short**: The vajram SDK uses code generation extensively to make developers' life easy by making data access type-safe. The id of the vajram is used as a prefix of all generetated classes related to the vajram. So a long vajram name will lead to even longer class names. For example, if a vajram's name is 'getAndUpdateUserNameFromUserService`, krystal generates the following classes:
      1. `public class GetAndUpdateUserNameFromUserServiceRequest`
      2. If Vajram has input modulation disabled:
         1. package-private `class GetAndUpdateUserNameFromUserServiceFacets`
      3. If vajram has input modulation enabled
         1. package-private `GetAndUpdateUserNameFromUserServiceModInputs`
         2. package-private `GetAndUpdateUserNameFromUserServiceCommonFacets`
         3. `public class GetAndUpdateUserNameFromUserServiceImpl`
   4. **Vajram names are globally unique**: Unlike class names which areunique within a package, vajram names are supposed to be globally unique. This allows vajrams to be reusbale across various services and deployments. So name the vajram accordingly. This also means that refactoring vajrams by moving the java file to a different package, module or repo does not impact the vajram Id.
