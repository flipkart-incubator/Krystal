# The Krystal Programming Paradigm

## Author: [Ram Anvesh Reddy Kasam](mailto:ram.anvesh@flipkart.com)

# Introduction

In this paper, we try to answer the following question: 

***Are today's high level programming languages adequate for solving the challenges of building modern distributed software systems?***

The answer, we conclude, is: No, they are not.

We take a critical look at today's high level programming languages in light of all the latest developments in the domain of distributed software systems, modern day business, product, and tech demands on such systems and our learnings from building large scale distributed systems at Flipkart \- what these languages offer, what they don't, and what they *should* offer. How they are structured, and how they *should* be. What kind of code, coding practices and patterns they directly or indirectly encourage, and how these can be improved, etc.  We know from the work of Alonzo Church and Alan Turing, that all modern Turing-complete languages are essentially, functionally equivalent \- meaning a task which can be accomplished by one Turing-complete language can definitely be accomplished by any other turing-complete language. So the area of focus here is not on the **what** question (the functional capabilities of modern high-level languages) but the ***how*** question (how do these languages accomplish a task and what are the implications of the design choices that the languages make).

Doing this, admittedly, is no small task \- some would also say foolhardy even. But, hey\! Isn't that what's fun about it?

The best way to approach this task is to embark on a journey. This journey will start from simplicity (cute little programs which we wrote when we started learning programming) and move towards complexity (the real word \- messy, ugly and nasty code with all the kinks and idiosyncrasies that we have to deal with on a day to day basis in our day jobs). The idea is this \- throughout the journey we will use the same programming language and observe how it morphs and moulds and contorts itself to satisfy our changing needs, constraints and environments. 

So…

# … Let the journey begin …

For sake of brevity we will write code in a single language (Java) throughout this journey. But almost all of the observations and arguments we make are language-agnostic and apply to almost all modern high level programming languages.

## Prologue: We the protagonists

We are fresh out of college, and have been placed in a brand new ecommerce startup. We learnt C, Java and Python in college (especially for the placements :P)  and are comfortable with all the programming language fundamentals (like Objects, polymorphism,  inheritance etc.) and Design patterns (like decorator, adapter etc.). We have been assigned the backend team which is supposed to provide data to the mobile app, and are thrilled to learn that Java is the lingua franca here. With a lot of excitement, we start our journey.

## Chapter 1: The initial functional requirement

On our first day, we are asked to implement a piece of software called "getProductDetails" which implements the following algorithm:

1. Accept the product Id (a string) as input  
2. Fetch data from ProductDB, and return the product details.

Simple enough; we implement the code as follows:

| import com.mycompany.ProductDB;  /\*\* \* Sample calling code:  \*  \* ProductDetails pd \=     \*     new ProductDetailsFetcher(productId).getProductDetails(); \*/public class ProductDetailsFetcher {		private String productId;      private ProductDB productDB;	public ProductDetailsFetcher(String productId){		this.productId \= productId;		this.productDB \= ProductDB.instance();      }	public ProductDetails getProductDetails(){		return productDB.getProductDetails(this.productId);      }} |
| :---- |

.. and immediately send this for code review to a senior team member, who is also our mentor, hoping to merge and deploy to production ASAP\!

But unfortunately we are asked to make some changes…

## Chapter 2: Dependency Injection

Our mentor explains to us that the above code is not easily testable because it doesn't allow you to mock the DB instance and write unit tests. To allow for this you need to accept the DB instance as a constructor argument and let the runtime decide which DB instance is passed: Actual DB in production and test DB in unit tests. 

"let the runtime decide which DB instance is passed"? How? What does that even mean? \- we scratch our heads. Our mentor smiles knowingly, and asks us to read about [Dependency Injection](https://en.wikipedia.org/wiki/Dependency_injection) and to use that pattern here.  
So, our first day goes into reading about and understanding dependency injection. And as we read we realize the power of this pattern. Just inject your dependencies in your constructor with a jakarta.inject.Inject annotation, define the object creation bindings in a central place which is reusable across classes and we are done\! The same production code can be unit tested without making DB calls. All those huge object creation code we wrote in our college assignment could have been avoided\! We learn the difference between data plane and control plane and the need to keep them separate. The data-plane should contain the business logic \- and the control plane should contain non-business logic code like object creation bindings. We are so excited that we learnt something new\! A little bewildered as to why this design pattern was not taught in college, but nevertheless \- this is a great start at the new job\!

So the next day, we make the necessary code changes:

| import jakarta.inject.Inject;import com.mycompany.ProductDB;public class ProductDetailsFetcher {		private String productId;      private ProductDB productDB;            @Inject	public ProductDetailsFetcher(String productId, ProductDB productDB){		this.productId \= productId;		this.productDB \= productDB;      }	public ProductDetails getProductDetails(){		return productDB.getProductDetails(this.productId);      }} |
| :---- |

But this doesn't work\! The injected constructor accepts one parameter injected by the runtime (the ProductDB) and another provided by the calling code (productId). This is not allowed by the dependency injection framework.

So the second day goes into reading about [Assisted injection](https://dagger.dev/dev-guide/assisted-injection.html) \- and why it is needed and how it's supposed to be used. We will have to replace the @Inject annotation with an @AssistedInject annotation, annotate the productId param with the @Assisted annotation\! And not only that, we need to create a new factory interface which has a method with only the productId param and the interface needs to be annotated with @AssistedFactory:

| import jakarta.inject.Inject;import com.mycompany.ProductDB;import com.diframework.Assisted;import com.diframework.AssitedInject;import com.diframework.AssistedFactory;public class ProductDetailsFetcher {            @AssistedFactory      public interface ProductDetailsFetcherFactory {        ProductDetailsFetcher create(String productId);      }	private String productId;      private ProductDB productDB;            @AssitedInject	public ProductDetailsFetcher(              @Assisted String productId, ProductDB productDB){	    this.productId \= productId;	    this.productDB \= productDB;      }	public ProductDetails getProductDetails(){	    return productDB.getProductDetails(this.productId);      }} |
| :---- |

Now our clients can't just call us like before:  
new ProductDetailsFetcher(productId).getProductDetails();   
They will have to inject the  ProductDetailsFetcherFactory and then do:  
ProductDetailsFetcherFactory.create(productId).getProductDetails();

This is a bit confusing to us. If the data plane is supposed to have only business logic, why this new factory interface which is not part of my core algorithm? Why so many annotations instead of just one? Why should my client's call patterns be affected by my decision to use dependency injection? Our business requirement did not change \- then why did our code change? Our mentor nods understandingly and tells us that these concerns are valid. But when building modern enterprise-scale systems, we need to adopt some patterns, which, like it or not, happen to have these side-effects.

### Discussion

(Once in a while in this journey we will step out of the shoes of the protagonists to have deeper discussion about the issues at hand.. these are the ***Discussion*** sections )

At first glance it might seem like the issue here is the design of the dependency injection framework. But that is not the case. The underlying issue here is that the programming language is not aware of the dependency injection pattern. This is the reason the compiler is not able to reason at the client's call site that some parameters are provided and some are injected, and just passing only the providable params should suffice \- and compile successfully \- doing away with the factory altogether. According to the programming language, every line of code is part of the data plane \- there is no control plane within the language. Those of us who have built large scale systems will know how important this segregation is. Delegating this to a framework outside the language definition comes at a cost. Also, dependency injection is just one of the problems which are solved by the control plane vs data plane separation as we will see later in the journey. 

At this point, we shall summarize this discussion as a design guideline item so that we can revisit all these at the end of the journey and see what we get when we combine all these design guidelines.

#### **DesignGuideline\#1**: A truly modern programming language should have a native understanding of dependency injection.

*Turns out some programming languages have learnt this lesson\! [Scala has "using" clauses](https://docs.scala-lang.org/scala3/reference/contextual/using-clauses.html) to solve this exact problem.*

Now, back to our journey…

## Chapter 3: The experience of being choked\!

After many such new learnings and experiences, our code is finally deployed\! All team members contributed various features and now we are ready for a load test of the backend. But during the load test, we realise something bad is happening. The service is not able to scale. Beyond a certain QPS, the servers stop taking requests and just hang \- there is no CPU being used, but no new requests are being processed. All incoming requests start timing out. This makes the complete team sleepless. After much debugging, we realise that the issue, among other things, is in the ProductDetailsFetcher class\! What we realise is that we are directly calling the database in the same thread. At load, the DB is a bit more latent than normal. Also, some product details are extremely large in size and can take more time to retrieve. This causes the incoming thread to block on the DB call longer. This causes a cascading effect and eventually browns out the service \- which is no longer able to serve even products which have small data sizes. Not only this, other APIs in the same service which are more important to business than getProductDetails, like getPrice also stop responding due to the same reason. So this is how it feels to be choked by our own threads…

Thanking the gods that this issue was caught in a load test and not in production, we begin fixing the issue. One simple way to do this is to increase the number of threads in the server. But this only delays the inevitable. To truly fix this issue, we need to make the DB call in a thread from a dedicated thread pool with its own pool size. This way, if the getProductDetails query is latent, only those threads in the dedicated pool will be used up and the other threads will remain free to do other important work. We need to run load tests and perform some calculations taking DB latency and load estimations into account to determine the optimal pool size. Then we need to code in such a way that when the thread pool is exhausted, we instantly reject all requests to that DB so that incoming threads are not blocked unnecessarily, so that this DB call doesn't penalize other APIs which do not depend on this DB call. Not only this, we add a timeout to the DB get call to have an upper limit on how much time the DB can make us wait, so that bad requests do not penalize good requests- this is the "fail fast" idiom. After this, the code looks like this:

| import jakarta.inject.Inject;import com.mycompany.ProductDB;import com.diframework.Assisted;import com.diframework.AssitedInject;import com.diframework.AssistedFactory;public class ProductDetailsFetcher {          @AssistedFactory    public interface ProductDetailsFetcherFactory {      ProductDetailsFetcher create(String productId);    }    private String productId;    private ProductDB productDB;    private ExecutorService threadPool;          @AssitedInject    public ProductDetailsFetcher(            @Assisted String productId,             ProductDB productDB,            ExecutorService threadPool){        this.productId \= productId;        this.productDB \= productDB;         this.threadPool \= threadPool;    }    public ProductDetails getProductDetails() throws        RejectedExecutionException,         ExecutionExceptoin,         InterruptedException {            return executorService.submit(() \-\>                 productDB                .getProductDetails(this.productId))                .get(20, SECONDS));    }} |
| :---- |

Submitting a task to a threadpool brings with it three possible checked exceptions which can be thrown. Should we catch these exceptions, or declare them in throws? Catching and handling all of them is so much extra code. But if we don't do it, then our clients will have to do it\! The funny thing here is that even with all this extra code, the underlying business requirement has not changed. Is it right that we have to be making all these changes? Modern languages like Java allow us to reduce this code by using the Aspect Oriented Programming (AOP) pattern in which we can annotate the method in question with a special annotation which tells the AOP framework to wrap the method in a threadpool call. This avoids polluting the business logic:

| import jakarta.inject.Inject;import com.mycompany.ProductDB;import com.diframework.Assisted;import com.diframework.AssitedInject;import com.diframework.AssistedFactory;public class ProductDetailsFetcher {            @AssistedFactory      public interface ProductDetailsFetcherFactory {        ProductDetailsFetcher create(String productId);      }	private String productId;      private ProductDB productDB;            @AssitedInject	public ProductDetailsFetcher(              @Assisted String productId, ProductDB productDB){	    this.productId \= productId;	    this.productDB \= productDB;      }       @ExecuteInFixedPool(30)       @Timeout(time= 20, units \= SECONDS)	public ProductDetails getProductDetails(){	    return productDB.getProductDetails(this.productId);      }} |
| :---- |

where `@ExecuteInFixedPool` is a hypothetical AOP-enabled annotation. But this comes with its own limitations:   
1\. How do we release the DB connection when the timeout expires? If we don't do this, the DB connection pool will choke\! To do this, we will need to make sure that the thread in which the DB call is made is interrupted on timeout and hope that the DB client library handles that interrupt properly by releasing the connection.

2\. The timeout is static. What if the `getProductDetails` method is called in different contexts which have different acceptable timeouts? To do this, our calling code will have to set the deadline in the ThreadLocal and the AOP framework will have to read the request deadline from there so that the timeout can be calculated and used to do a timedGet from the Future returned by the threadpool. Not only this, it will have to also copy the deadline in the thread-local of the thread in the fixed thread pool, because our code is running on a different thread, and code inside ProductDB might need that deadline value to set network read timeouts\!  Not only that, after the DB call is finished, we will have to unset the deadline from the thread-local because this thread is a pool thread and would be used to process other requests which will have their own deadlines\! This thread-local copy over will have to be done every time any step in the call path delegates to another threadpool. 

To solve the problem of copying and unsetting of threadlocal variables, the latest Java versions have introduced the concepts of [virtual threads](https://openjdk.org/jeps/444) (which are extremely lightweight and do not need to be pooled in threadpools as they are use-and-throw) and [scoped values](https://openjdk.org/jeps/464) (which are auto-copied to child threads). While this infinite pool of virtual threads is great, their unpooled nature makes them unsuitable for isolating the damage of latent queries from affecting other parts of the code. To solve this problem, we need to use semaphores in the AOP code instead of threadpools.

### Discussion

The combination of AOP, virtual threads, and scoped values significantly reduces the boiler plate needed to achieve isolation of tasks in the runtime. Thanks to these modern features, the amount of changes we need to make to the business logic are extremely small. This chapter teaches us something important. Concerns like resource pools (threadpools and semaphores) and execution controls (like deadlines/timeouts) are better left out of business logic \- they impact how your tasks are executed \- not the data that the tasks return. This is another example of the need for data plane and control plane separation. PS: While virtual threads help us out here \- they come with other disadvantages as we will see in a later chapter

#### **DesignGuideline\#2**: A truly modern programming language should have a native understanding of the difference between control plane and data plane.

## Chapter 4: A new functional requirement\! {#chapter-4:-a-new-functional-requirement!}

After the successful launch of our e-commerce application, we get a new requirement from the product team:  
*Do not return any  product details from product DB, if the product is unavailable to be bought.*  
This task is assigned to us \- and this makes us super excited \- we will be able to use the existing code that we have written for a new use case\!  
We leave the existing `ProductDetailsFetcher` as it is for backward compatibility, and write a new `AvailableProductDetailsFetcher` which uses, or depends on, the ProductDetailsFetcher.

| //imports..public class AvailableProductDetailsFetcher {            @AssistedFactory      public interface AvailableProductDetailsFetcherFactory {        AvailableProductDetailsFetcher create(String productId);      }      private String productId;      private ProductDetailsFetcher productDetailsFetcher;       private AvailabilityDB availabilityDB;            @AssitedInject      public ProductDetailsFetcher(              @Assisted String productId,                ProductDetailsFetcherFactory productDetailsFetcherFactory,               AvailabilityDB availabilityDB){	    this.productId \= productId;	    this.productDetailsFetcher \=                productDetailsFetcherFactory.create(productId);	    this.availabilityDB \= availabilityDB;      }      @ConcurrencyLimit(30)      public Optional\<ProductDetails\> getProductDetails(){          if(availabilityDB.isProductAvailable(productId)){	        return Optional.of(                  productDetailsFetcher.getProductDetails());          } else {              return Optional.empty();          }      }} |
| :---- |

While this code is functionally correct, over time we discover an issue with this implementation \- an increase in latency is causing customer engagement to trend downwards. It is clear why this latency increase is seen. It's because of the sequential call to `isProductAvailable` and `getProductDetails` methods. Business and product teams are not OK with this behaviour. So we are given a follow-up task to bring the latency back to the previous behaviour. The only way to do this is to parallelize both the DB calls. So, we get to work. But how do we do this? We speak to our mentor and explain the problem to them. They tell us that we need to offload the blocking calls to another threadpool. The moment we call `isProductAvailable`, the current thread blocks. So we cannot call it directly. We need to call it in a different virtual thread so that we can call getProductDetails parallelly. Taking all this in, we make changes so that the code looks like this now:

| //imports..public class AvailableProductDetailsFetcher {  @AssistedFactory  public interface AvailableProductDetailsFetcherFactory {    AvailableProductDetailsFetcher create(String productId);  }  private final String productId;  private final ExecutorService virtualThreadsExecutor;  private final ProductDetailsFetcher productDetailsFetcher;  private final AvailabilityDB availabilityDB;  @AssitedInject  public AvailableProductDetailsFetcher(      @Assisted String productId,      ProductDetailsFetcherFactory productDetailsFetcherFactory,      AvailabilityDB availabilityDB,      ExecutorService virtualThreadsExecutor) {    this.productId \= productId;    this.virtualThreadsExecutor \= virtualThreadsExecutor;    this.productDetailsFetcher \=        productDetailsFetcherFactory.create(productId);    this.availabilityDB \= availabilityDB;  }  @ConcurrencyLimit(30)  public Optional\<ProductDetails\> getProductDetails() throws Exception {    Future\<Boolean\> isAvailableFuture \=        virtualThreadsExecutor.submit(() \-\>           availabilityDB.isProductAvailable(productId));    ProductDetails productDetails \=       productDetailsFetcher.getProductDetails();    if (isAvailableFuture.get()) {      return Optional.of(productDetails);    } else {      return Optional.empty();    }  }} |
| :---- |

While it's a bit unfortunate that in order to achieve parallelization, we had to re-introduce the executor service which we avoided by using AOP in the previous chapter, we are happy that we were able to reduce the latency significantly and contribute to improved business metrics.

While the above code works well in most scenarios, in 0.01% of the scenarios, it throws an error. A couple of weeks later, this point is brought to our team's attention. We debug and find out that `isProductAvailable` DB call is failing in 0.01% of the requests. While this is not a deal-breaker, this can be avoided \- the product team tells us to assume that the product is available in case the call to the DB fails. This must be a small change \- all we have to do is catch the exception and provide a default value. This time, we assign this task to our mentee \- a newly minted engineer who just joined the team. We proudly explain the code to them and ask them to make the changes. They make the changes, add a test case to check if the defaulting behaviour is working, and submit a PR. We review the code, check that all existing and new unit tests are succeeding \- which they are. We approve the PR, merge and trigger deployment. We congratulate our junior team mate for reducing these errors from a few 1000s per day, to zero\! They are very happy, and so are we. This is the new code:

| //imports..public class AvailableProductDetailsFetcher {  //.. same as before  @ConcurrencyLimit(30)  public Optional\<ProductDetails\> getProductDetails() {    Future\<Boolean\> isAvailableFuture \=        virtualThreadsExecutor.submit(() \-\>           availabilityDB.isProductAvailable(productId));    boolean isAvailable;    try {      isAvailable \= isAvailableFuture.get();    } catch (Exception e) {      isAvailable \= true; // Error handling    }    ProductDetails productDetails \=       productDetailsFetcher.getProductDetails();    if (isAvailable) {      return Optional.of(productDetails);    } else {      return Optional.empty();    }  }} |
| :---- |

But there's a problem. If you have caught the issue, then you must be  a great code reviewer\! When this code gets deployed to production, the latency increase we solved a couple of weeks back, re-emerges causing a reduction in some key business metrics. Also, the increased latencies triggered some client timeouts which further impacted the availability and business metrics of our app. Our team oncall gets paged, we scramble and immediately rollback the deployment to mitigate the issue.

Then we debug the issue and find the problem \- the `isAvailableFuture.get()` should have been called *after* the `productDetailsFetcher.getProductDetails()` call. While refactoring, we missed this \-  the developer making the change was not part of the team when the latency reduction was done. And the code reviewer (that's us) somehow missed this \- it was just adding a try catch and error handling \- we didn't review it from the perspective of concurrency flow control. 

This production issue will need a root cause analysis (RCA) to be done. RCAs are done to get to the core underlying issues in systems and processes so that they can be fixed, and issues like these don't repeat. When we do an RCA for this issue we realize that all the guardrails were followed \- all existing unit tests succeeded, code review was done. But still the issue happened. Unfortunately unit test cases only check functionality (are we getting the right result), not non-functional aspects like how much time is it taking to get the result, what is the flow of concurrent code, etc. For that we will have to set up extensive load testing frameworks which run all functionality in an A-B setup (old code vs new code) and report any non-functional anomalies. Us, being a startup, with such a small team and so many new features to build, this is a huge task for the team. So this gets added to our backlog, waiting to be picked up… some day…

All this aside, we are concerned for our mentee. Causing a production issue with our very first deployment is not a good experience\! They are dejected to have caused this. We are even more distraught since it was our code, we assigned the task, and we did the review. We take our young teammate for a samosa chai to the cafeteria and try to console them \- it's not their fault, things like this happen, we learn from these mistakes and become better. We had context about the code and its history and we should have caught it in the review, so we are equally, if not more, responsible. 

Then our mentee asks us this question:  
In the code that we deployed…

|     try {      isAvailable \= isAvailableFuture.get();    } catch (Exception e) {      isAvailable \= true; // Error handling    }    ProductDetails productDetails \=       productDetailsFetcher.getProductDetails(); |
| :---- |

… the `productDetailsFetcher.getProductDetails()` line of code has no data dependency on the `isAvailable` variable being computed in the try catch block \- these two are completely independent of each other. Then why didn't the runtime just run them concurrently and avoid this issue? We explain to them that unfortunately that is not how programming languages work. Even if these two lines of code have no explicit dependency on each other, they are implicitly linked \- because every line of code written is assumed to have an implicit dependency on the previous line of code \- it cannot execute until the previous line completes. Because of this assumption, the compiler and language have no way to know if the developer wants to avoid calling `getProductDetails` until `isAvailableFuture.get()` is called or if it's a bug \- "this, unfortunately", we say to them "is the cost of doing business".

This time it's our teammate's turn to sigh. Both of us look at each other, shrug and start walking back to our seats \- but in our mind, the question still lingers \- "Is it really possible for the programming language to have avoided the issue by inferring the independence of the two pieces of code?"

### Discussion

Experienced developers would be very familiar with the situations described in the chapter \- performing a small refactoring that changes the performance characteristics of the program, invisible to the unit tests, frantic rolling back from production on performance degradation, etc. But we put up with this because we have been trained to think that solving problems like these are the responsibility of the testing infrastructure, not the programming language\! It's not possible for programming languages to execute code concurrently by inferring inter-statement dependencies to avoid instances where we block unnecessarily \- right?

The truth is, this is definitely possible. Programming languages were designed decades ago to solve one main problem: "How do we encode instructions which need to be executed on a CPU core" \- emphasis on  "a CPU core" \- implying exactly one core. And CPU cores are, by design, linear instruction executors \- the only way they could execute code is linearly. This is how code was written even \- as a linear set of statements (remember [BASIC](https://en.wikipedia.org/wiki/BASIC)?). At that time, this was an acceptable design which modelled the underlying hardware architecture decently well.

54 years ago, in his seminal paper "[Notes on structured programming](https://www.cs.utexas.edu/users/EWD/ewd02xx/EWD249.PDF)", Djikstra introduced [Structured programming](https://en.wikipedia.org/wiki/Structured_programming) to improve the quality and readability of code \- this helped introduce `for` loops, function calls, and other code structures into programming languages \- so that code could be organized better, and didn't have to look like a big essay \- one statement after another.

When computers started getting more cores, and operating systems added support for OS threads, programming languages added abstractions into the language to support these features, but did not change the language itself. for loops and function calls from one function to another added better code organisation and readability, but they did not change the fundamental assumption \- all statements are executed sequentially. All subsequently introduced languages unfortunately stuck with this assumption.

Today, it's not just more cores, it's many computers \- each with many cores which are needed to perform one single operation. Want to add a product to your cart? Scores of cores across computers coordinate with each other over a network to make this one "simple" task happen. In the world of modern distributed computing, it is only fair to expect programming languages \- which are the basic building blocks of computation \- to evolve to adapt to this reality. Just like structured programming, we need a new paradigm in programming languages \- this is called [Structured Concurrency](https://en.wikipedia.org/wiki/Structured_concurrency). Many frameworks are being built within existing programming languages towards this goal ([nurseries for python](https://vorpus.org/blog/notes-on-structured-concurrency-or-go-statement-considered-harmful/), [structured concurrency for java](https://openjdk.org/jeps/462)), but like existing abstractions like Futures and Promises, these run the risk of a combination of not solving all the problems to the full extent, and introducing new problems which don't exist today. 

To summarize:   
Linear instruction execution is one-dimensional in nature:

What we need is a programming language which has has the ability to model two-dimensional code:

This "two-dimensional" code is what achieves concurrency.

#### **DesignGuideline\#3:** A truly modern programming language must have the ability to natively, structurally model two-dimensional code.

## Chapter 5: The cost of concurrency

After the RCA is done, we fix the code and redeploy it:

| //imports..public class AvailableProductDetailsFetcher {  //.. same as before  @ConcurrencyLimit(30)  public Optional\<ProductDetails\> getProductDetails() {    Future\<Boolean\> isAvailableFuture \=        virtualThreadsExecutor.submit(() \-\>           availabilityDB.isProductAvailable(productId));     ProductDetails productDetails \=         productDetailsFetcher.getProductDetails();    boolean isAvailable;    try {      isAvailable \= isAvailableFuture.get();    } catch (Exception e) {      isAvailable \= true; // Error handling    }    if (isAvailable) {      return Optional.of(productDetails);    } else {      return Optional.empty();    }  }} |
| :---- |

At last, this code works as expected. But we are a bit flabbergasted. All we had to do was to parallelize two network calls. And this is what we ended up with:

1. We ended up increasing the amount of code in the method from 6 lines to 16 lines\!   
2. We had to introduce an executor service and submit tasks to it manually. While we were able to avoid doing this using AOP in chapter 3, this is no longer possible.   
3. We are introduced to a new concept called "Future". We have to learn how Futures work, their behaviour, API etc.  
4. We are using the `get()` method on the Future which potentially blocks indefinitely. In this case it is not an issue because the `isProductAvailable` method has an AOP wrapper which times out the call based on a deadline. But it's easy to see how error prone this is. To someone reading this class, it might seem like a bug at first glance.  
5. Having to do this everytime we need to parallelize network calls like `availabilityDB.isProductAvailable` pollutes call sites like `AvailableProductDetailsFetcher`. 

Some of the above problems (1, 2, 5\) can be mitigated if the `isProductAvailable` method itself returns a future. This way the parallelization can be centralized so that every client of `isProductAvailable` doesn't have to submit tasks to executor services. This same logic applies to `getProductDetails` as well. This indirectly means that all APIs which make network calls MUST return Futures. But the unfortunate side effect of this is that even classes like `AvailableProductDetailsFetcher` will have to return a Future so that clients of this class can parallelize these calls when needed as well. 

This leaves us with the following design options across our codebase \- 

1. Enforce that all tasks which make network calls and all tasks which directly or indirectly depend on such tasks MUST return Futures so that parallelization can be achieved by those who need it. This has the unfortunate side effect that even clients who don't need parallelization will have to deal with the Futures.  
2. Enforce that all tasks which make network calls and all tasks which directly or indirectly dependant on such tasks MUST have two APIs : one which returns a Future and one which returns plain objects. This allows clients that need parallelization to use the Future-returning APIs and clients that don't need this call the Plain-Object-Returning APIs. The unfortunate consequence of this option is the extremely large API surface that needs to be maintained by the code owner and learnt by the user.  
3. All tasks just return plain objects and its client's responsibility to submit tasks to executor services when parallelization is needed. The unfortunate consequence of this option is repetition of code and unnecessary verbosity in many call sites.

Each of these options are sub-optimal in one way or the other. And none of these options even solve some of the problems mentioned above (3,4).

We sigh, wondering if things really have to be this complicated with as simple a requirement as making parallel calls. But, given that our experienced and talented mentor told us that this is the cost of doing business, so to speak, we shrug, and move on.

### Discussion

Things definitely don't need to be this complicated. Let us take a simple example. Let us imagine there is a way for us to annotate a method which tells the runtime that this method makes a network call and the runtime allows clients to choose how they call the same method \- either in sync fashion which returns a plain Object return type and or async fashion which returns a future \- without the developer of the method having to write two methods with different return types. For this to be possible, the language must understand the difference between a piece of code which runs on the current thread vs a piece of logic which offloads computation outside the current thread (via a network call, for example). With this native understanding, the language compiler can analyze the call site and figure out if the caller is calling in a sync/async context and call the relevant version of the method. This avoids the need to choose one of the 3 sub-optimal options. The underlying problem with modern programming languages is that they do not have a native understanding of when computation is offloaded(delegated away) from the current thread. Instead, they rely on sub-optimal abstractions like `java.util.Future`, Promises, Observables and other such abstractions which are added to languages as add-ons that are supposed to act as substitutes for this missing feature of the programming languages \- but they never can, because they are not built in to the grammar of the language, and so always will leave rough edges like the ones discussed above (This is applicable to almost all modern programming languages).

But this is just a flavour of what is possible with a programming language which works at the right level of abstraction. We can do much better, like eliminate the need to use futures altogether \- but we are getting ahead of ourselves \- we will revisit this a bit later in the journey. For now summarizing this learnings from this chapter:

#### **DesignGuideline\#4:** A truly modern programming language must have language-native understanding of which code runs on the current thread, and which code delegates the computation outside the thread (a different thread running on a different core, or an IO socket to access the disk or a network hosted API, etc) so that the language compiler can intelligently adapt to the needs of the developer without either the code owner or the code caller having to write repetitive, verbose code.

Before we return to our journey, let's spend a couple of minutes more to absorb this better- because this is extremely important. If we look at the abstraction provided by the `java.util.Future` class, it is a way to represent the concept of a 'delegated computation' using the abstraction of an 'Object'. And with the support of generics, `Future<T>` can represent delegated computations which can return any data type. But `T` is a generic type without bounds. So can `T` be a `Future` itself? According to the java compiler it definitely can \- no compiler errors will be thrown when a type `Future<Future<String>>` is used in code, for example. But according to the semantics of the 'delegated computation' concept, a 'delegated computation of a delegated computation' doesn't make any sense. A computation is either delegated, or it's not. So a `Future<String>` is the same as a `Future<Future<String>` which is the same as a `Future<Future<Future<String>>>` and so on.. So ideally we should be able to assign each of these to any of the others. But the compiler can't allow it because it treats these as objects, not as delegated computations. Or better still, the language should not even allow us to represent these nested futures because they are redundant. This predicament is a hint to us that the language is missing something.

Does the above reasoning ring a bell? Does this remind you of another instance where an 'Object' abstraction is being used where it should ideally not be needed? Well, if you got it, congratulations. It's the `java.util.Optional` object. Just like futures, `Optional<Integer>` is the same as `Optional<Optional<Integer>>` and so on.. 'Nullable types' is the abstraction that the language needs to have, lacking which we are forced to use optionals, which the compiler treats as objects. Maybe we should put this in as a design guideline as well.

#### **DesignGuideline\#5:** A truly modern programming must have a native understanding of something like 'nullability' so that developers can succinctly deal with nullable types with the help of the compiler instead of using objects wrappers or other abstractions

(Kotlin is an example of a language which does this to some extent using the `?` operator. But as we will see later, we can take this a step further)

## Chapter 7: A non-functional requirement: reduced cost {#chapter-7:-a-non-functional-requirement:-reduced-cost}

As our new e-commerce app becomes more successful, we see a lot of growth in the number of requests that our backend systems need to serve. While we are able to meet the demand by horizontally scaling our services, this comes as a cost. For the first time since the app's launch, our backend team has been asked to take a break from launching feature after feature, and instead, we have been asked to reduce the cost of running the service. Since we are serving demand from end users, the number requests we are receiving is not in our control. So the only way to reduce cost is to reduce the cost of serving each request. We start analysing our metrics and try to come up with a way to do this. Eventually we realize that the problem with the current code is the way the DB is being called. Everytime we call productDB and availabilityDB, we are passing a single input \- productId. We are doing this even when a page in the app has dozens of products \- all of which need to be queried to render the page. These single-input calls are concurrent, so there is no negative impact on the latency of the requests, but this call pattern leads to a extreme chattiness over the network between the backend app and the DB and also, the DB is not able to optimize the way it accesses and retrieves data from its cache and its solid-state persistence. So the next step is clear \- we need to change our code to use the batching APIs of the DB client library instead of the single-input APIs. This way, the number of network calls, the number of cache retrievals and the number of disk IO operations will reduce many times over and give us good performance.

Let's get to work\! We have two classes that we know access the DB clients. THey are the `ProductDetailsFetcher` which accesses the productDB and the `AvailableProductDetailsFetcher` which accesses the availabilityDB. Let's first change the `ProductDetailsFetcher`. Instead of using the API `productDB.getProductDetails`, we need to call the `productDB.getProductDetailsBatch` which accepts a list of productIds and returns a list of `ProductDetails` objects \- one for each productId.

| public class ProductDetailsFetcher {  @AssistedFactory  public interface ProductDetailsFetcherFactory {    ProductDetailsFetcher create(List\<String\> productIds);  }  private final List\<String\> productIds;  private final ProductDB productDB;  @AssitedInject  public ProductDetailsFetcher(      @Assisted List\<String\> productIds,       ProductDB productDB) {    this.productIds \= productIds;    this.productDB \= productDB;  }  @ConcurrencyLimit(30)  public Map\<String, ProductDetails\> getProductDetails() {    return productDB.getProductDetailsBatch(this.productIds);  }} |
| :---- |

We change the DB client method and accordingly change the data types of the productId constructor param, field, and factory param to accept a `List` of `String`s. Similarly, we change the `AvailableProductDetailsFetcher` class:

| public class AvailableProductDetailsFetcher {  @AssistedFactory  public interface AvailableProductDetailsFetcherFactory {    AvailableProductDetailsFetcher create(List\<String\> productIds);  }  private final List\<String\> productIds;  private final ExecutorService virtualThreadsExecutor;  private final ProductDetailsFetcher productDetailsFetcher;  private final AvailabilityDB availabilityDB;  @AssitedInject  public AvailableProductDetailsFetcher(      @Assisted List\<String\> productIds,      ProductDetailsFetcherFactory productDetailsFetcherFactory,      AvailabilityDB availabilityDB,      ExecutorService virtualThreadsExecutor) {    this.productIds \= productIds;    this.virtualThreadsExecutor \= virtualThreadsExecutor;    this.productDetailsFetcher \= productDetailsFetcherFactory.create(productId);    this.availabilityDB \= availabilityDB;  }  @ConcurrencyLimit(30)  public Map\<String, ProductDetails\> getProductDetails() {    Future\<Map\<String, Boolean\>\> isAvailableFuture \=        virtualThreadsExecutor.submit(() \-\>              availabilityDB.areProductsAvailable(productIds));    Map\<String, Boolean\> isAvailable \= new HashMap\<\>();    Map\<String, ProductDetails\> productDetailsMap \=          productDetailsFetcher.getProductDetails();    try {      isAvailable \= isAvailableFuture.get();    } catch (Exception ignored) {    }    Map\<String, ProductDetails\> result \= new HashMap\<\>();    for (Entry\<String, ProductDetails\> e : productDetailsMap.entrySet()) {      String productId \= e.getKey();      ProductDetails productDetails \= e.getValue();      if (isAvailable.getOrDefault(productId, true)) { //Error-handling        result.put(productId, productDetails);      }    }    return result;  }} |
| :---- |

Again, we make all the relevant changes to accept `List<String>` for productIds. Not only this, since the `ProductDetailsFetcher` returns a `Map`, we need to explicitly iterate through these maps to retrieve the ProductDetails after also querying the `Map` returned by availability.

What's more, these changes are contagious. Not only did we have to change the two classes which make the DB calls, we will have to make these same changes in every other class in the code base which directly or indirectly depend on either of these classes\! 

What's worse still is that the above change does not achieve optimal batching. Because the batch of productIds is determined by the caller, if two different calling classes invoke `AvailableProductDetailsFetcher` for two different use cases within the context of the same request, with 5 productIds each, we have no way to batch these two batches into a single batch of 10\! Which means that all these contagious code changes which we did throughout the codebase involving dozens of classes (`getProductDetails` is a core functionality of ecommerce after all), all we have achieved is a best-effort batching implementation \- the only requests which can be batched in the above implementation are the ones which have the exact same call path. For example, let us say we have a call path `classA -> classB -> classC -> getProductDetails` All the products received by classA as input can be batched together. But if we have an alternative call path in the same request `classD -> classE -> classC -> getProductDetails`, there is no way to batch calls across these call paths. 

This is disappointing to say the least. Not a single functional requirement has changed. We ended up making changes to 100s of lines of code to batch every call to the product DB, not to mention changing all the test cases, and all we get is a suboptimal "best-effort" batching implementation which doesn't comprehensively solve the problem at hand. Our mentee is confused. We know why. What we saw in this chapter flies in the face of one of the most important design principles that has been drilled into us repeatedly over the years \- separation of concerns. The performance improvement was to do with batching \- the way our backend application calls the DB over the network. This means that, ideally, only the DB client libraries should have had to change. Why did we instead have to change even classes which don't directly access the DB client?

### Discussion

Many programming languages make this almost impossible. Once the new batch methods are introduced into the DB client, the only way a caller can opt-in to this optimization is to make significant code changes to use this new method. If a programming language had a way to truly separate the two concerns \- the functional need to process data, and the non-functional need to batch DB calls, we could have avoided so many code changes. Also if the programming language runtime has a way to "collect" inputs from multiple call paths and batch them together and then dispatch the results to each caller, we could have achieved optimal batching. Some programming languages runtimes like javascript run on an [eventloop](https://www.youtube.com/watch?v=8aGhZQkoFbQ). So they can technically achieve this. In these runtimes, the DB clients can collect productIds till the end of the current "[tick](https://nodejs.org/en/learn/asynchronous-work/understanding-processnexttick)" of the eventloop and then dispatch the DB calls to achieve close-to-optimal batching. To achieve this, the DB client methods must necessarily be asynchronous in nature since they accept requests at one point in time, but dispatch them later when all requests in the current tick have been collected. The changes to make a sync method call an asynchronous one are also contagious, unfortunately. But if the methods were already asynchronous, the code changes in the call sites would be close to zero. This shows us that programming language design can mitigate such problems like optimal batching where non-functional changes in one module leak and impact all the direct and indirect call sites.

Let's take a real world example. The javascript [Graphql Dataloader](https://github.com/graphql/dataloader?tab=readme-ov-file#batching) relies on the above "ticking" behaviour of the js runtime to achieve batching where all pending calls at the end of the current eventloop are batched. But the [java port](https://github.com/graphql-java/java-dataloader) of the same library [does not have this luxury](https://github.com/graphql-java/java-dataloader?tab=readme-ov-file#manual-dispatching). For this reason, in the java world, developers are expected to "wait for an appropriate time" to collect requests and then manually dispatch the batch so that at least some cross-callpath batching is achieved. These two approaches have their own pros and cons. The ticking implementation takes away control from the application owners. The batching parameters like dispatch time and batch size are completely delegated to the runtime. Also, the ticking behaviour only works for "level 1 network calls"  \- these are calls to APIs which can be made immediately after the application receives the request. But real-world call graphs are more complex \- they also have "level 2 network calls" which can be made only after we receive a response from some of the "level 1" network calls (maybe because some of the outputs are needed as inputs to these calls). In some scenarios, some call paths to an API are "level 1" and some others are "level 2" \- for example, the inputs for productId "P1" are readily available, but the inputs for productId "P2" need to be retrieved from a batched network call to another API. The ticking implementation cannot batch across these two types of calls. On the other hand, the "wait and collect" implementation gives control to the developer as they can decide how much time to wait for each network call, but this comes with the overhead of figuring out this optimal time which can be very hard \- especially for complex call graphs. If the time is too low, the batching is sub-optimal. If the time is too high, then request latencies will increase. Also the java implementation returns a map of results \- we have already discussed the problems with this change \- the "contagiousness" that this causes across the codebase. The most important takeaway from this chapter is that all these problems are not non-negotiable tradeoffs. An ideal programming language can solve all of these problems:

#### **DesignGuideline\#6**: A truly modern programming language must provide a way to implement batching in a way that adheres to the design principle of "separation of concerns". It should allow us to add batching as a capability to a piece of code without leading to any "contagious" code changes across call sites. The batching must be truly optimal in two ways: No batchable inputs must be left behind, and no time should be spent unnecessarily waiting for batches to get filled. The batching configuration should work for all kinds of call graphs (containing network calls at "level 1", "level 2", and so on…) and application owners should have substantial control over the batching policy and parameters.

## Chapter 8: The cost of decoupling

As we scale up our operations and our backend team grows, we start to create domain specific teams so that they can innovate and evolve independently. As part of this, a new engineering team called "Product Catalog" team is created. This team wants to expose an API which other systems can call to fetch product details. To do this, we would have to make the following changes: 

* Move the code of `ProductDetailsFetcher` to their service  
* Reimplement the `ProductDetailsFetcher` to accept an injection of the ProductCatalogService client instead of the productDB, and convert the response of the service into a `Map`:

| public class ProductDetailsFetcher {  @AssistedFactory  public interface ProductDetailsFetcherFactory {    ProductDetailsFetcher create(List\<String\> productIds);  }  private final List\<String\> productIds;  private final ProductCatalogServiceClient svcClient;  @AssitedInject  public ProductDetailsFetcher(      @Assisted List\<String\> productIds, ProductCatalogServiceClient svcClient) {    this.productIds \= productIds;    this.svcClient \= svcClient;  }  @ConcurrencyLimit(30)  public Map\<String, ProductDetails\> getProductDetails() {    return svcClient        .getProductDetailsBatch(this.productIds)        .found().stream()        .collect(Collectors.toMap(ProductDetails::productId, Function.identity()));  }} |
| :---- |

This code rewrite is an artefact of the fact that modern programming languages allow communications among various pieces of code only within a common runtime. This not only leads to unnecessary code changes when service boundaries change even if there are no functional changes, but also freeze the service boundaries globally. It is no longer possible to host `ProductDetailsFetcher` as a service in some contexts, but in other contexts, package it in the same runtime. If this was possible we could choose the right approach depending on the runtime characteristics of the client application (how chatty is the network call, serialization/deserialization cost et.) without the calling code needing any code changes, and without needing any additional application code.

### Discussion

This pattern is called the "modular monolith", where modules are written in such a way that there is clean separation, and clean interfaces, so that module A doesn't need to change in any way when module B which it communicates with is either co hosted or remotely hosted. 

#### **DesignGuideline\#7**: A truly modern programming language must allow to design code as modules which can be packaged in various ways just by changing build configurations, and need zero code changes.

## Chapter 9: And so the journey continues… 

The journey up until this point is just the tip of the iceberg. There are many many more functional and non-functional requirements we encounter on a daily basis when running high-scale distributed systems that can benefit from a "truly modern programming language", as we have been calling it, which provides new, higher-level computational abstractions that allow us to model the code using the right primitives \- so that developers can truly focus on modelling the domain and the business logic rather than spending time and effort bending over backwards to somehow contort age old primitives which were not design for this purpose to accommodate modern problems of distributed computing.

In this chapter, instead of going in-depth into one problem per chapter, we'll pick up pace. Let's do a quick walk through of more such problems which arise because modern programming languages and runtimes do not provide adequate abstractions to build distributed systems.

* **Timeouts vs Deadlines**: Managing timeouts for various service calls within an application can be a pain. Figuring out the right timeouts is an involved process needing extensive performance testing across services to figure out the right limits. Moving to deadlines and client-provided request timeouts reduces this overhead. In this approach, clients provide a timeout per request. The server deduces a deadline (a point in time) by which the complete call needs to succeed. This pattern is called "deadline propagation". Now all we have to do is to make sure this computed deadline is available to and respected by all points in the code where network calls are being made. Before the availability of virtual threads in the JVM, concurrency of network calls were achieved using multi-threading. Passing the deadlines in such a setup was painful \- involving writing custom code to copy over thread locals to new threads and then cleaning up threadlocals after the task execution. The issues with thread local usage have been well documented on the internet. Now with the availability of virtual threads and (soon to be available) [scoped values](https://openjdk.org/jeps/464), we can set the deadline as a scoped value which is passed to all child virtual threads. While this improves the situation to some extent, it doesn't solve the problem completely. We have spoken about Level1 and Level2 calls where Level2 calls' inputs are derived from Level1 calls' outputs \- In such a situation, what happens if a Level 1 call takes up all the time until the deadline? The Level2 call will have zero time left, leading to guaranteed failure. This is not an issue if the Level1 call is mandatory. But if the Level1 call is optional, then it would have made much more sense to fail the Level1 call much before deadline expiration so that the Level2 call could have performed some error handling and continued execution. Doing this reliably in current programming languages is not easy. Developers will have to retrieve the current deadline, manually divide the available time into two chunks: one for the Level1 call and one for the Level2 call, then submit the Level1 call in a by setting the reduced deadline as a scoped value. For example, if we did the above in `getAvailableProductDetailsFetcher`, the code would look like this:

| public class AvailableProductDetailsFetcher {  //.. same as before  public Map\<String, ProductDetails\> getProductDetails() {    double timeRatioForAvailabilityCall \= 0.5;    Future\<Map\<String, Boolean\>\> isAvailableFuture \=        virtualThreadsExecutor.submit(() \-\>            ScopedValue.where(                DEADLINE,                 getNewDeadline(timeRatioForAvailabilityCall))            .call(() \-\>                 availabilityDB.areProductsAvailable(productIds)));    Map\<String, Boolean\> isAvailable \= new HashMap\<\>();    Map\<String, ProductDetails\> productDetailsMap \=         productDetailsFetcher.getProductDetails();    try {      isAvailable \= isAvailableFuture.get();    } catch (Exception ignored) {    }    Map\<String, ProductDetails\> result \= new HashMap\<\>();    for (Entry\<String, ProductDetails\> e : productDetailsMap.entrySet()) {      String productId \= e.getKey();      ProductDetails productDetails \= e.getValue();      if (isAvailable.getOrDefault(productId, true)) { //Error-handling        result.put(productId, productDetails);      }    }    return result;  }  private static long getNewDeadline(double ratio) {    long currentDeadline \= DEADLINE.get();    long currentTime \= System.currentTimeMillis();    long timeRemaining \= currentDeadline \- currentTime;    //noinspection NumericCastThatLosesPrecision    return (long) (currentTime \+ (timeRemaining \* ratio));  }} |
| :---- |

  Even with the above implementation, we have not completely solved the problem, or to put it another way, we have introduced a different problem. The above solution only works if IO call concurrency is achieved via virtual threads where each network call is executed in a dedicated virtual thread and there is a clean thread-hierarchy of parent and child threads (this is needed because the deadline scoped values are passed from parent to child). But, as we discussed in the chapter concerning batching, to achieve optimal batching, we would need to collect requests from multiple callers, and then dispatch them together in a single thread. Here, unfortunately, the parent-child thread relationship breaks down. The actual thread making the network call is not a child of any one caller thread, each with its own deadline scoped value. So we will have to write logic which accepts all the deadlines from caller threads, and then computes one acceptable value \- the max of all the deadlines (this is because we don't want one caller which is ok with a higher deadline to be penalised because of a caller with lower deadline). This, unfortunately, means that the above code no longer works as expected. Because the deadline is the max of multiple deadlines, it is possible that the code above ends up giving more time to the availability call than intended. This means the developer will also have to implement a timeout logic so that the availability call times out in time. 

  What we are doing here is an example of "bending over backwards" \-  we use an abstraction (virtual threads) to solve one problem (concurrency) but then another requirement (batching) interacts with the assumptions inherent to the abstraction in such a way that they no longer apply and we go into another loop of finding another abstraction to solve the new resultant problem. This should be avoidable. It would have been great if the programming language allowed us to just annotate the network call dependency with something like `@TimeAllocationRatio(0.5)` and the deadline is auto-computed and transferred in such a way that batching implementation and other such implementations interact with each other smoothly.

* **Retry on error**: What if we want to retry some network calls for a limited number of times on failure? This is another example of a use case where annotating key interaction points would be helpful. The naive way of a doing this is to catch exceptions and put the calling code in a for loop:

| public static \<T\> T retry(Callable\<T\> callable, int retryCount)    throws Exception {    Exception exception \= null;    for (int i \= 0; i \< retryCount; i++) {      try {        return callable.call();      } catch (Exception e) {        exception \= e;      }    }    throw exception;} |
| :---- |

  This however works only if the callable encapsulates a blocking network call. If the network call returns a Future, we need a different retrying implementation \- which can be error prone \- we need to make sure we don't block on the futures prematurely (See [Chapter 4](#chapter-4:-a-new-functional-requirement!)). This retry logic will be different yet again if the returned value is a `CompletableFuture` and so on.. Not to mention how this retying mechanism interacts with other implementations like deadline. It would be great if the programming language allowed us to just annotate the call site with something like `Retry(3)` and let the runtime figure out how to do this.

  #### **DesignGuideline\#8**: A truly modern programming language runtime must have a native understanding of deadlines and the language must allow developers to annotate key points of interaction in the code (like ones where a network call is made) with metadata like expected time allocation, retry count etc.,  which can be used by the runtime to optimally orchestrate the call graph.

* **Data races and lock overhead**: In many scenarios, application code mutates state. While it's better to avoid state mutation as much as possible, it is not always avoidable. For example, during feature experimentation using AB experimentation, application code might update a common data structure with AB flags so that the response generator can use this to report AB experiment values via response headers, etc. This requirement unfortunately negatively interacts with virtual thread implementation. Different parts of the application code updating a common data structure from multiple application threads can cause data races (this is an issue we actually faced in production). To avoid such issues, we now need to make all such data structures thread-safe by implementing locking or synchronization. This comes with its own cost which we would like to avoid in systems which serve such high throughput of requests. Synchronizing global data structures which can be accessed across requests is understandable. But the fact that we are having to make request-scoped data structures (like response header collectors) synchronized is a code smell, to say the least. It tells us that using parallelism (virtual threads) to solve the problem of concurrency is not the right solution.

  #### **DesignGuideline\#9**: A truly modern programming language should allow modelling highly concurrent code without resorting to threads and parallelism so that we can avoid the resultant overheads we face due to locking and synchronization

* **Unintended performance degradation**: We already saw the performance impact because of premature blocking on futures in Chapter 4\. Similarly, performance degradations are possible:  
  * Due to adding new network calls: If we introduce a network call to an existing API from a new call site, it is possible that we forget to batch it, and this causes one additional network call to that API while it could have been batched in an existing network call. While unit tests and integration tests won't show any problems, On deployment, the traffic to that API could potentially double instantly. Unfortunately such issues are caught pretty late (When a company wide performance regression test is run \- before a huge sale, for example). When such issues are caught, we scramble to fix the issue and migrate to batched API, and add the new call to the existing batch.  
  * Due to unbounded fanout of IO calls: If a developer introduces a network call in a loop (we call this a "fanout"), it is possible that in rare scenarios, the loop bound is so huge that it completely degrades the performance of the current service and the service being called.

  It would be great if we could catch such issues at build time itself. For this to be possible, the programming language should allow modelling key points in the code, like the ones which originate network calls, in such a way that build time tools can construct the complete emergent call graph of the whole application and fail build in situations where new call paths are introduced without being batched, or put hard fanout bounds on places where multiple network calls are made to the same API.

  #### **DesignGuideline\#10**: A truly modern programming language should allow developers to write code in a way that allows build-time construction and validation of the emergent call graph so that possible performance degradations can be called out before code merge instead of detecting issues in production.

At this point, it is imperative for us to ask \- what if we build a "truly modern programming language" from scratch taking all the above design guidelines into consideration? From all the lessons we have learnt building, evolving and maintaining large scale distributed systems, can we come up with a programming language design which doesn't force us to compromise because of sub-optimal or unsuitable abstractions?

# Towards a new programming language…

Let us try and design this brand new programming language which aims to solve all the problems we mentioned above using *optimally calibrated abstractions*.

*Note: the following programming language is currently a proposal. A fully running implementation of it doesn't exist yet.*

## Vajram \- the smallest unit of work

This new programming language, called "**vajram-lang**", is built upon the concept of a "unit of work" which we call a "**vajram**". Vajrams are unique in that they have clear separation of control plane and data plane code. Vajrams have well-defined type-safe, immutable facets. These facets include 

* **inputs**: Vajrams take in zero or more inputs \- these are the functional signature of the vajram.  
* **dependencies**: a vajram can invoke other vajrams (called dependencies). But unlike traditional functions or methods, these invocations cannot happen in the data plane. Instead, vajram invocations are defined in the control plane \- and the runtime decides when to trigger the invocation.   
* **injections**: These are values provided to the vajram by the runtime environment (not its client vajrams)

Inputs, dependencies and injections are units of data which the vajram needs for it  to complete its operation. The control plane allows developers to write pure concurrent business logic just using code blocks \- and thus devoid of concurrency abstractions like Futures. This is achieved by allowing developers to model a two dimensional directed-acyclic-graph (DAG) within the vajram using code blocks which stitch the various facets together. The DAG outputs a single value which we call the **output** facet of the vajram. 

That's enough theory, let's look at an example. Let us start with the customary Hello world program

## Hello\! World…

**package** com.flipkart.krystal.vajram.lang.samples.helloWorld;

**void** helloWorld(**@inject** ConsoleWriter writer){  
 {  
   writer.Out.println("Hello\! World…")  
 }  
}  
An interesting thing to note here is that the `println` line is inside not just one, but two pairs of curly braces. This allows the vajram to have both control plane and data plane code \- more on this later. Let's look at a more complicated, yet familiar, example:

## Compute delegation (ProductDetailsFetcher)

Here we write the above `ProductDetailsFetcher` java code as a vajram. (To be concise, the java class has been migrated to a record)  
Java:

| public record ProductDetailsFetcher(     @Assisted String productId,       ProductDB productDB) {            @AssistedFactory      public interface ProductDetailsFetcherFactory {        ProductDetailsFetcher create(String productId);      }       @Concurrency(30)	public ProductDetails getProductDetails(){	    return productDB.getProductDetails(this.productId);      }} |
| :---- |

Vajram:  
ProductDetails? getProductDetails(  
  **@input string** productId,  
  **@inject** ProductDB productDB){

  **@output @Concurrency(30)** \~{  
    **return** productDB.getProductDetails(productId);  
  }  
}

Let us understand the above code.

* `ProductDetails`: This tells us that `ProductDetails` is the return type of the vajram. `ProductDetails` is a data carrier object similar to classes in other languages like java and python.   
* `?` : This tells that the value returned by this vajram is of an **errable type**. This is needed because the DB call might fail due to various reasons. Errable types indicate to the compiler and the developer that the value can be in one of three states:  
  * success: A value is present  
  * nil: No value is present \- indicates a missing value  
  * failure: No value is present and an error is present. This indicates that a value could not be computed because of the error.  
* `getProductDetails`: is a developer-given name of the vajram.  
* `@input` : annotation indicating the declaration of an input facet.  
* `string`: is the data type of the facet  
* `productId`: is the developer-given name of the facet  
* `@inject`: annotation indicating the declaration of an injection facet \- these are facets which are injected by the runtime and not the clients (invokers) of this vajram. The clients don't even see the injection facets as part of the signature of the vajram.  
* `ProductDB productDB`: Data type and name of the injection facet  
* `{` : The beginning of the control plane of the vajram  
* `@output`: Indicates that the following code block computes the output of the vajram. The code block is called the **output logic** and the returned value is called the output facet. One vajram can have at most one output logic and exactly one output facet.  
* `@Concurrency(30)`: Custom annotation indicating that the following code block cannot be executed with a concurrency exceeding 30\. (All annotations beginning with an upper-case letter are custom annotations. All Annotations beginning with a lower-case letter are language-native annotations)  
* `~` :  The **soon** operator indicates that the following code block **delegates computation** outside the current calling  thread (to another thread, to IO, to another process etc.). A very important point to know: the output logic is the only part of the vajram which can delegate computation, and hence can use the delegation operator.   
* `{` : open output logic \- start of the data plane  
* `return productDB.getProductDetails(productId);` : call getProductDetails method on the productDb object, and return the resultant value. (Similar to other programming languages)  
* `}`:  close output logic \- end of the data plane.  
* `}`:  close vajram \- end of the control plane.

We can make the code more concise: 

* `@input` is the default annotation on facets which don't have any other annotation  
* `@output` is the default annotation for code blocks like the one above.  
* `@Concurrency(30)` : This annotation makes the concurrency count static. The language allows some annotations to be added to facets and logics of a vajram at runtime. This way we can dynamically change such control config without application restart, and vary this configuration per request if needed. So we can remove this annotation from the source code.  
* The last statement of any data plane code block is allowed not to end with a `;`. Such a statement is considered to be the return statement of the code block.

The above code can be shortened as  
ProductDetails? getProductDetails(  
  **string** productId,  
  **@inject** ProductDB productDB){

 \~{  
   productDB.getProductDetails(productId)  
 }  
}

Similarly, we write an `isProductAvailable` vajram which delegates to the availability DB over network:

**bool**? isProductAvailable(  
  **string** productId,  
  **@inject** AvailabilityDB availabilityDB){

 \~{  
   availabilityDB.isAvailable(productId)  
 }  
}

Points to note:

* The `?` in `bool?` indicates that `isProductAvailable` might fail or return `nil`.  
* This code doesn't show us the return type of `availabilityDB.isAvailable`. If `availabilityDB.isAvailable` returns a concurrency wrapper like `Future`, `CompletableFuture`, `Promise`, `Observable` etc, then the platform automatically treats this as a [non-blocking](https://www.geeksforgeeks.org/blocking-and-nonblocking-io-in-operating-system/) compute delegation. On the other hand, if `isAvailable` doesn't return a concurrency wrapper, but returns some other data type, then the language runtime treats this as a blocking call and creates a new virtual thread in which the output logic is executed. Either way, the runtime makes sure that the execution thread never blocks on the output logic. This is an important feature of the vajram runtime \-  communication across vajrams is executed in a single thread called the execution thread. Virtual threads are created only when `~` (compute delegation) is encountered.  
* Both the above vajrams have inputs, injections and outputs, but no dependencies. We shall see vajrams with dependencies next.

## Dependency declaration

With `getProductDetails` and `isProductAvailable` vajrams ready, we can now write `getAvailableProductsDetails` vajram which depends on these two vajrams:

ProductDetails? getAvailableProductDetails(  
   **string** productId){

  **@dep bool**? isAvailable \=  
     isProductAvailable(productId \= productId.trim());

  **@dep** ProductDetails productDetails \=  
     getProductDetails(productId \= productId.trim());

  {  
    **if**(isAvailable?default(**false**)) {  
      productDetails  
    } **else** {  
      **nil**  
    }  
  }  
}

Points to note:

* `getAvailableProductDetails` has one input but no injections  
* It returns an `Errable`(`?`) which means this vajram may return a `nil` or failure even if all its mandatory dependencies succeed.  
* This vajram has two dependencies:   
  * `isProductAvailable` vajram: whose return value is stored in the facet `isAvailable`. The dataType of `isAvailable` is an errable(?) which means `isProductAvailable` is an optional dependency. Even if it fails, the execution of this vajram can continue.  
  * `getProductDetails` vajram: whose return value is stored in the facet `productDetails` of data type `ProductDetails`. This is not an `Errable` (because `?` is not present). This means that `getProductDetails` is a mandatory dependency. If it fails, then `getAvailableProductDetails` fails instantly with the same error.  
* `isProductAvailable` vajram's input `productId` is assigned to the input facet of this vajram: `productId` after trimming it. Same is true with `getProductDetails`. The `productId.trim()` code is part of the data plane \- it can be any arbitrary complex piece of logic which uses other facets. This logic is called the resolver logic because it resolves the inputs of the dependency. This vajram has two resolvers. One which computes the `productId` of `isProductAvailable` and one which computes the `productId` of `getProductDetails`  
* Both the dependency definitions are in the **control plane** of the vajram \- This means if, when and how the calls to the dependency vajrams are made is not in the control of the developer \- it's the runtime's responsibility to figure it out.  
* Since both the dependency facets depend on the input facet `productId`, and no other facet, the language runtime is able to figure out that these dependency vajram invocations are independent of each other, hence they are invoked **concurrently.** It might seem counterintuitive to us since we are used to traditional high level languages which execute statements in sequence. But these invocations are part of the control plane, and the control plane is not linear in nature \- it is a DAG which is built by the language runtime by analysing what facets are being used by each dependency. So the language runtime is able to invoke the dependencies in the most optimal order.  
* Since `isAvailable` is an optional facet, all uses of this facet must handle the scenario that its value is missing. This is done in the output logic:  `isAvailable?default(false)`. Normally the `.` operator is used to access methods and fields of a type. In this case we use `?` instead which allows us to use methods of the `Errable` type. The `default` method converts an errable type to a non-errable type by assigning it a value in case there is no value or its an error.  
* The output logic doesn't have a `~` since the computation is not delegated \- the computation is run in the execution thread itself.

Again, the above code can be made more concise.

* `@dep` is optional  
* when using the `bool?` type, we can skip `default(false)` \- the compiler will infer this for us \- since false is the default value for booleans.  
* `if ... else` can be used as a ternary operator

ProductDetails? getAvailableProductDetails(  
    **string** productId){

  **bool**? isAvailable \=  
     isProductAvailable(productId \= productId.trim());

  ProductDetails productDetails \=  
     getProductDetails(productId \= productId.trim());

  {  
    **if**(isAvailable?) productDetails **else nil**  
  }  
}

Summarizing the execution flow, `getAvailableProductDetails` invokes `isProductAvailable` and `getProductDetails` concurrently by forwarding its input `productId` to both these vajrams. If `getProductDetails` fails or returns `nil`, then this vajram does the same immediately \- its output logic is not executed. This is because  `productDetails` is a mandatory dependency. When `getProductDetails` succeeds and `isProductAvailable` completes (either with success or failure, since it's optional), the output logic is executed and the return value of this logic is returned to the invokers of this vajram.

The control plane is the code which is directly inside the outermost set of `{}`. The data plane is the code which is inside the nested `{}`. Control plane execution is controlled by the language; for example, the decision of when a dependency vajram is invoked is the language runtime's decision. Data plane execution is sequential like traditional programming languages. 

## Conditional dependency execution

To see the power of this structure with control plane and data plane separation, let's see how some changes in our backend systems impact our code. Let's say, we have heavily optimized the `isProductAvailable` and `getProductDetails` calls by adding caching layers. This means that even if we call them sequentially, the new combined latency will be the same as before. We use this to our advantage \- we can shield the `getProductDetails` call behind the `isProductAvailable` call to reduce the load on the productDB. In [chapter 4](#chapter-4:-a-new-functional-requirement!) we saw how such a change impacts the code in traditional programming languages. We either need to do away with Futures and call the blocking avatars of the methods \- which is a big change, or we need to move/inline the `getProductDetails` call into the final if block.

| //imports..public class AvailableProductDetailsFetcher {     //… same code as above  @ConcurrencyLimit(30)  public Optional\<ProductDetails\> getProductDetails() {    Future\<Boolean\> isAvailableFuture \=        virtualThreadsExecutor.submit(() \-\>           availabilityDB.isProductAvailable(productId));    boolean isAvailable;    try {      isAvailable \= isAvailableFuture.get();    } catch (Exception e) {      isAvailable \= false; //Error handling    }    if (isAvailable) {      return Optional.of(         productDetailsFetcher.getProductDetails());    } else {      return Optional.empty();    }  }} |
| :---- |

For anyone reading the code, to infer that the `getProductDetails` is shielded by the `isProductAvailable` is not obvious at first glance \- we need to follow the code path to understand the control flow \- this is not very developer friendly.

Let's see how the vajram code changes in this scenario:  
ProductDetails? getAvailableProductDetails(  
    **string** productId){

  **bool**? isAvailable \=  
     isProductAvailable(productId \= productId);

  ProductDetails productDetails \=   
    getProductDetails(productId \= productId)  
      **@skipIf** { isAvailable? \== **false** };

  {  
    **if**(isAvailable?) productDetails **else nil**  
  }  
}

Because the vajram doesn't deal with futures or other such concurrency wrappers, we have nothing to remove from the code to make the calls sequential. All we have to do is to add a `@skipIf` data code block to the control plane in which we use the `isAvailable` facet to decide whether to skip `getProductDetails`. The language runtime infers that the output of one dependency is used in the resolution of the other, so the invocations automatically become sequential. The key difference here is that the control flow is not based on the order of statements. Instead it is determined by the content of the code in the data plane. (To make the code more readable, a code linter can reorder the dependency definition order to reflect the topological order of execution to make the code more reader friendly \- this doesn't change the functionality of the code). This makes a huge difference when the number of dependencies is very high or the logic is very complicated. This allows code writers and readers to localise their thought process when reasoning through code instead of trying to follow a control flow in their head as to when which line will be executed, especially when multiple parallel control flows of different method calls are interleaved in the same sequence of statements.

## Mandatory, Optional and default values

Let's continue with code changes. What if we want to loosen the mandatory dependency on `getProductDetails`? i.e we want to return a default ProductDetails value if `getProductDetails` fails.

Java:

| //imports..public class AvailableProductDetailsFetcher {    //... same code as above   @ConcurrencyLimit(30)  public Optional\<ProductDetails\> getProductDetails() {    Future\<Boolean\> isAvailableFuture \=        virtualThreadsExecutor.submit(() \-\>              availabilityDB.isProductAvailable(productId));    boolean isAvailable;    try {      isAvailable \= isAvailableFuture.get();    } catch (Exception e) {      isAvailable \= false; // Error handling    }    if (isAvailable) {      ProductDetails productDetails;      try {        productDetails \= productDetailsFetcher.getProductDetails();      } catch (Exception e) {        productDetails \= new ProductDetails(productId);      }      return Optional.of(productDetails);    } else {      return Optional.empty();    }  }} |
| :---- |

Vajram:  
ProductDetails getAvailableProductDetails(  
   **string** productId){

  **bool**? isAvailable \=  
     isProductAvailable(productId \= productId.trim());  
    
  ProductDetails productDetails \=  
     getProductDetails(productId \= productId.trim())  
       ?default{ **new** ProductDetails(productId) }  
       **@skipIf** { isAvailable? \== **false** };

 {  
   productDetails  
 }  
}

Since the output logic has no logic as such \- it just forwards the value of a dependency facet, we can skip writing the output logic by annotating the facet with `@output`. Also, the `@skip` annotation accepts a reason parameter which acts as documentation and error message for when the dependency is skipped.

ProductDetails getAvailableProductDetails(  
   **string** productId){  
   
  **bool**? isAvailable \=  
     isProductAvailable(productId \= productId.trim());

  **@output**  
  ProductDetails productDetails \=  
     getProductDetails(productId \= productId.trim())  
       ?default{ **new** ProductDetails(productId) }  
     **@skipIf**("Product is not available")  
     { isAvailable? \== **false** };  
}

This is how a developer would read and understand this vajram:

* `getAvailableProductDetails` vajram returns a non-errable `ProductDetails`  
* It takes a single `string` input called `productId`  
* It has a dependency on `isProductAvailable` vajram which is invoked by passing the `productId` input to its input. The return value of this dependency vajram invocation is represented by an errable boolean facet called `isAvailable`  
* This vajram also has a dependency on `getProductDetails` vajram which is also invoked by passing `productId` to it. If the vajram invocation fails or returns an empty value, then a default value is to be used.   
* If `isAvailable` is false, then `getProductDetails` invocation is to be skipped and the default value is to be used.  
* The result of this dependency vajram invocation is to be stored in the facet `productDetails` and this value is also to be used as the output of this vajram.

## Language-native batching {#language-native-batching}

When we introduced batching in [Chapter 7](#chapter-7:-a-non-functional-requirement:-reduced-cost), it led to a huge amount of code changes \- not just in the parts of the code which actually needed batching, but all code which directly or indirectly depended on this code. vajram-lang prevents this kind of "contagiousness" of code changes. Let us introduce batching in the `getProductDetails` vajram:  
ProductDetails? getProductDetails(  
 **@batch**(type=\`*Batch*\`, name=\`*batches*\`)   
 **string** productId,  
 **@inject** ProductDB productDB){

 \~{  
   List\<String\> productIds \=  
       *batches*  
       .map{\_.productId()}  
       .toList();  
   BatchResults\<*Batch*, ProductDetails\>\~ productDetails \=  
     productDB.getProductDetails(productIds)  
       \~.stream()  
       \~.map(**new** Batch(\_.productId()), \_)  
 \~.collect(toBatchResults());

   productDetails  
 }  
}

vajram-lang provides native support for batching. We tag the inputs that we want batched using `@batch` annotations. This annotation has the additional capability to interact with, and modify the behaviour of the language type system and runtime. This `@batch` annotation tells the vajram-lang compiler that even though the input types and output types of the vajram have not changed, there is a change in the way the output logic consumes the inputs and produces the output. Specifically, it says that all the inputs annotated with `@batch` can be accessed by the output logic only via an object containing these inputs . An iterable of these objects is accessible via the *'`batches`'* facet which is generated by the annotation processor. The tag also tells the compiler and runtime that while the return type of the vajram remains `ProductDetails?`, the output logic will return a special data structure which maps each batch (represented by the *`Batch`* data type) with a `ProductDetails?` (i.e `BatchResults<Batch, ProductDetails>`). The runtime then makes sure that each caller of this vajram gets the `ProductDetails?` object(s) corresponding to the inputs that they provided \- for example, if a client vajram provides just one `productId` it would receive just one `ProductDetails?`. Most importantly, this is how the language runtime is able to prevent contagious changes \- by making adding/removing batching support in a vajram a backward compatible change since the vajram's functional signature isn't affected. Hence, all the other vajrams which depend on this vajram remain unchanged \- they are unaware that this vajram has started executing its output logic with batched inputs \- that remains an internal detail to this vajram. Any future changes like opting out of batching, adding more inputs to the batch, removing input from the batch \- are guaranteed not to affect any other downstream vajrams.

At execution time, the language runtime allows for a plugin (let's call it a batcher) to be registered with the runtime. This plugin takes the responsibility of processing the inputs and creating these "batches". How these batches are created is an implementation detail of the batcher. For example, a naive batcher can implement a timer which waits for a specific amount of time to collect batches \- this would be similar to the java implementation of the graphql dataloader we spoke about in the discussion section of [Chapter 7](#chapter-7:-a-non-functional-requirement:-reduced-cost). More sophisticated batching implementations which achieve 100% optimal batching with zero time wastage are possible. A small note on "plugins": These plugins (the "batcher", for example) are called "decorators". Decoration is a core capability provided by the vajram-lang runtime. Decorators can enhance and add capabilities to the vajram (especially non-functional capabilities). A Vajram can opt-in to these decorators via annotations (`@annotationExample`). More on Decorators in a later section.

## Errability

The `?` operator indicates that a data type is errable \- meaning it might have a missing/`nil` value with an optional error representing the reason the value is not present. This errability is native to the language's type system.

**void** errabilityDemo() {

  *// this dependency might fail*  
  **string**? errableFacet \= tryGetString();

 {  
   *//returns "" if value is absent*  
   errableFacet?default("");

   *// returns true if value is present*  
   errableFacet?valuePresent();

   *// returns true if value is absent*  
   errableFacet?valueAbsent();

   *// returns true if both value and error are absent.*  
   errableFacet?isNil();

   *// returns true if error is present*  
   errableFacet?errorPresent();

   *// returns true if error is absent*  
   errableFacet?errorAbsent();

   **nil**?default("fallback");     *// returns "fallback"*  
   **nil**?valuePresent();  *// returns false*  
   **nil**?valueAbsent();   *// returns true*  
   **nil**?value();         *// returns nil*  
   **nil**?isNil();         *// returns true*  
   **nil**?errorPresent();  *// returns false*  
   **nil**?errorAbsent();   *// returns true*  
   **nil**?error();         *// returns nil*

   **string**? test \= "test";  
   test?default("");     *// returns "test"*  
   test?valuePresent();  *// returns true*  
   test?valueAbsent();   *// returns false*  
   test?value();         *// returns "test"*  
   test?isNil();         *// returns false*  
   test?errorPresent();  *// returns false*  
   test?errorAbsent();   *// returns true*  
   test?error();         *// returns nil*

   **string**? error \= **err**("test error");  
   error?default("fallback");     *// returns "fallback"*  
   error?valuePresent();  *// returns false*  
   error?valueAbsent();   *// returns true*  
   error?value();         *// returns nil*  
   error?isNil();         *// returns false*  
   error?errorPresent();  *// returns true*  
   error?errorAbsent();   *// returns false*  
   error?error();         *// returns err("test error")*  
 }  
}

## Concurrency primitives \- Delegation and Futures

vajram-lang supports concurrency as a native construct. As we saw before, the `~` operator before code block: `~{}` indicates that the logic in the code block delegates computation outside the execution thread, when used with data types, it indicates that the value will be available at a future point in time. For example java data types like `Future<Map<K, V>>` are defined instead as `Map<K,V>~`. The `~` indicates that the map value will be available at a future point in time, not immediately after the line is executed. This allows developers to chain method calls on these "futures", just as if they normal variables, without actually blocking on the delegated logic:

**int\~** nameLength=   
    productDB  
        .getProductDetails("productId")  
       \~.productName()  
 \~.length();

The `~.` operator is similar to the `.` operator \- it allows us to access the methods of the data type, but with the caveat that these methods are to be executed when the original variable is fulfilled. The datatype of the return value also has the `~` operator. Omitting this will throw a compilation error since the compiler knows that a "delegated call chain" must return a "future" value. This same code if written in java would look like this, assuming the `.getProductDetails` call returns `CompletableFuture<ProductDetails>`:

| CompletableFuture\<Integer\> productNameLength \=   productDB       .getProductDetails("productId")       .thenApply(ProductDetails::productName)       .thenApply(String::length); |
| :---- |

The `thenApply` method is an unfortunate artefact of the fact that the language doesn't understand futures natively. Because of this developers have to learn the syntax and usage of the `CompletableFuture` class API which can be very complex \- this should not be necessary. Further, If we make a change here \- that the `productName` returns a `CompletableFuture<String>` instead of a `String`, the java code will change like this:

| CompletableFuture\<Integer\> productNameLength \=   svcClient       .getProductDetails("productId")       .thenApply(ProductDetails::productName)        .thenCompose(Function.identity())       .thenApply(String::length); |
| :---- |

Since the `productName` method returns a `CompletableFuture<String>`  The additional `thenCompose` call needs to be added to convert a `CompletableFuture<CompletableFuture<String>>` into a `CompletableFuture<String>`. The vajram code on the other hand doesn't need to change since the language's type system coerces the nested futures into a single level future.

## The control plane

The part of the code inside the outer `{}` and outside the inner `{}` is called the control plane of the vajram. The control plane serves the purpose of declaring the data flow of the vajram via dependencies and dependency resolvers (logics which compute inputs to the dependencies). Traditional control flow constructs like loops  (e.g. `for` and `while`) are not allowed in the control plane.

This restriction is deliberate \- dependency definitions are locations where we delegate computation to other vajrams which can potentially make network calls, and have failure modes which are not typical to local function calls. Restricting the kind of code that we can write in the control plane allows the build system as well as the runtime parse this part of the code and make inferences regarding the "structure" of the overall application and allow useful validations, optimizations and maintain overall code and system health through automation which is not possible in code in which these restrictions are imposed. 

The restriction that iteration loops are not allowed in the control plane might seem extreme \- how do we call another vajram multiple times? Vajram-lang allows us to do this without loops. Let us assume we need to write a vajram which accepts a product collection object containing `N` productIds and we need to compute the average price of the products in the collection. For this we need to query the `getProductDetails` vajram `N` times. We cannot use iteration loops in the control plane; we can use iteration loops in the data plane, but we cannot invoke other vajrams there. So how do we achieve this? Vajram-lang supports dependency fanouts as a native feature where a vajram can declare a "fanout dependency" meaning that the dependency will be called multiple times depending on how many inputs are resolved by the resolvers. Let's take a look:  
**double**? computeAveragePrice(ProductCollection collection){  
  Fanout\<ProductDetails?\> productDetails \=\* getProductDetails(  
     productId \=\* collection.productIds();  
  )

  {  
    productDetails.values()  
      .stream()  
      .filter(\_?valuePresent())  
      .map(\_.price())  
      .mapToDouble()  
      .average()  
  }  
}

The `=*` operator means that the facet `productDetails` is a fanout dependency which contains the outputs of the multiple calls to `getProductDetails` vajram. Within the vajram call `productId=*` means that the vajram is invoked with `N` productIds where `N` is the size of the list returned by the RHS which is `collection.productIds()`. This means if the `collection` input has 23 productIds, then `getProductDetails` is called 23 times \- once for each product id in the collection. In simple non-fanout dependency, the data type of the dependency facet is same as the output type of the dependency vajram (`ProductDetails?`). But in this case, the data type is `Fanout<ProductDetails?>`. This indicates that the data type of the facet is a data structure which maps the input values sent to `getProductDetails` to the corresponding output. The output logic above accesses the list of all the dependency outputs (in the same order corresponding to the list of productIds in `collection`) by calling `.values()` on the `productDetails` facet.

## Logic Decorators

A brief note on logic decorators. vajram-lang provides a way to extend the capabilities of the execution runtime via Logic decorators. The Output logic decorator, for example, when applied to a vajram, intercepts every call to the vajram's output logic \- in other words it wraps the output logic. This allows us to implement capabilities like batching, circuit breaking, etc. Logic Decorators can contain their own state and their lifecycle can be bound to the current request (Request scoped decorators) or the application (session scoped decorators).

## Vajram call graph

The advantage of mandating that inter-vajram dependencies must be declared in the control plane in this specific way is that it gives a deep understanding of the structure of the application call flow to the runtime (and the build system). The runtime can model the code as a DAG where the nodes are vajrams and the edges are the dependency facets. All the vajrams written above will form the below vajram call graph

This call graph is visible both at compile time and run time. From this we can see that `getAvailableProducts` has an optional dependency (named `isAvailable`) on `isProductAvailable`, and a mandatory dependency on `getProductDetails`. Similarly, `computeAveragePrice` has a fanout dependency on `getProductDetails`. This graph view of the vajrams and their dependencies is a fundamental feature of vajram lang. It unlocks many capabilities that would be almost impossible or at least extremely hard to implement otherwise. Let us look at a few examples…

### Optimal Batching

In the above call graph, `getProductDetails` is being called from a couple of vajrams. As we discussed in [Chapter 7](#chapter-7:-a-non-functional-requirement:-reduced-cost) and in the section on [Language-native Batching](#language-native-batching), it is not trivial to achieve optimal batching in such a call graph where calls from `getAvailableProducts` and `computeAveragePrice` need to be batched. But in vajram-lang this becomes trivial. The "Batcher" logic decorator plugin is given a configuration listing the "call paths" which need to be batched together. In this particular case, the two call paths `getAvailableProductDetails:productDetails->getProductDetails` and `computeAveragePrice:productDetails->getProductDetails` are provided as "batchable" to the batcher module. As soon as the `getProductDetails` batcher receives inputs from both these callpaths, the batcher collects the inputs and dispatches the batch call to `getProductDetails`. This strategy allows us to batch Level1 calls, Level2 calls, and calls at any other level. And this is achieved without the application waiting for any additional time to batch. Since these batchable call paths are controlled by the application owner from outside the vajram code itself, optimal batching can be achieved. Not only this, the batchable call paths config is independent of the core application logic (logic inside the vajrams). This allows us to have different batchability configs for different applications which have the above call graph based on how optimal we want the batching to be. This allows application owners to tradeoff number of IO calls to batch sizes as needed without touching the functional application logic.

### Granular Execution Control

With such a call graph available to the runtime, application owners can apply metadata to the edges of the call graph to control various execution characteristics of the call graph. For example, the application owners can apply the retry metadata `@Retry(times = 3)`to the edge `getAvailableProductDetails:productDetails->getProductDetails` since we feel it's a critical callpath. This metadata can be used by a "Retrying Logic decorator" to retry `getProductDetails` thrice if it fails. Since this metadata is added to a single edge, it doesn't apply to the other incoming call path to `getProductDetails` from `computeAveragePrice`. This way we can tag critical call paths while leaving non-critical callpaths alone \- allowing us to pay the extra price of retrying only where needed, and without any code changes in the application logic. To achieve this in traditional programming languages, we would either have to make code changes at the call site to implement a retry loop, or make changes at the destination to blanket-retry all requests. Both these situations are undesirable. These same arguments apply to other resiliency constructs like Circuit Breaker, Concurrency limits, etc.

## Error handling

# "Krystalline" programming

Let us take a step back. The previous section describes the design of vajram-lang \- a hypothetical programming language which aims to solve many of the problems of writing business logic in distributed environments. More generally, this language can be seen as a particular implementation of a philosophy or a paradigm. The tenets of this paradigm are:

* Write business logic as a set of functions (logics)  
* Group functions which achieve a common goal into a module \- a unit of work (Ex: vajrams)  
* While the functions are stateless by definition, allow for the units of work to model type-safe immutable-after-set data as state (facets). The functions inside a unit of work read some facets to compute another facet.  
* The structure of these inter-facet dependencies should be visible to the application build system and runtime \- and this should allow the application to create an optimal execution plan for the code rather than the developer having to create the execution plan. Put differently, the logics should execute in a non-procedural, reactive manner.  
* Each unit of work must be the smallest compilation unit \- with its own interface, error codes, and internal state.  
* The runtime must have a first-class understanding of delegated computation (Ex: `~`) and in-thread computation.  
* Developers should not have to deal with concurrency constructs like Futures, Promises and Observables in majority of the cases \- they should just have to deal with pure data, and still have confidence in the correctness of the concurrency of the code they are writing.  
* Data transfer between units of work within a request must execute via message-passing in a single execution thread via an eventloop-like pattern.  
* The units of work must have a clear separation between control plane (where facet  definitions are declared) and data plane(where facets are computed). The declarations in the control plane must be parsable at compile time to build a visually-representable,  inter-module call graph.  
* The runtime should provide a way to write custom plugins which add capabilities to the application runtime (Ex: logic decorators).  
* The application code must have a clear separation between business logic and control logic (Ex: vajrams vs logic decorators)  
* The application runtime should be able to add metadata to nodes and edges of the call graph at runtime to modify the execution/performance characteristics of the execution plan without having to change the business logic and thus guaranteeing functional correctness of the business logic. As a corollary, various applications should be able to configure the performance characteristics of the same call graph differently based on their requirements without having to change the business logic (Ex: via annotations and tags).  
* Errors should be treated like data where the errors should flow through the call graph just like data through facets and inter-facet dependencies rather than special control flows like throw-try-catch etc. (Ex: Errables)

When code is written adhering to the above tenets, we say it follows the Krystal programming paradigm or simply that the code is "krystalline". The fundamental aim of the Krystal programming paradigm is to provide optimally calibrated abstractions to developers, so that they can write business logic with minimal ceremony, where the provided abstractions model realities of distributed systems, like failures, latencies, shared state and other such nuances which make programming for distributed systems in traditional programming languages painful.

But how are we to be sure that the above tenets and abstractions are optimal? How do we know that we are not-overfitting to what we know about distributed systems *currently* and the problems we have faced *recently,* i.e how do we eliminate recency bias from our design? Only if we are able to do this will we be able to have confidence that what we are building will have a long shelf-life instead of being another temporary software development trend (of which, you might agree,  there are many). 

Well, there is no fool-proof way to do this, but we can rely on some heuristics. One such heuristic is what can be called "the unreasonable effectiveness of good design" \- meaning a design which was built to solve problem 1, but somehow without fundamentally having to be rebuilt, solves problem 2, 3, 4 and so on… If the new problems are very similar to problem 1, then the fact that the original design solves those problems is not that surprising or "unreasonable". But truly good design must be able to solve problems which at first glance, or historically, have been considered fundamentally different from the original problem. If a design is built upon the right ("optimally calibrated") abstractions, it will end up solving other problems which seem completely unrelated to the original problem that was targeted. This happens because seemingly unrelated problems sometimes arise from common, generally invisible or unknown, underlying fundamentals which go unseen or overlooked. And the hypothesis here is that if we see a design exhibiting such behaviour, then we can be certain, to some extent, that we are on the right path.

The Krystal project initially began to solve one sub-problem of distributed systems \- specifically: concise error-free coding and optimal execution of synchronous microservices orchestration. But as we built this further, we have observed that the above programming tenets lend themselves very well to other problems of computation systems.

## Other applications

### Function as a Service and Distributed Execution Graph

Traditionally when we write microservices, we developers choose a service framework (Dropwizard, grpc), a wire protocol (json, protobuf), and then write APIs in the relevant service definition language. Then we publish our client libraries so that clients can call our APIs. To do this we need to start with system design/architecture as our first step \- we need to first divide the complete end-to-end business requirement into sub-domains, then define our service boundaries, we then divide these services into team owners, and then the owners write the code. Once the code is written, the service boundaries are burnt into the architecture of the system. If for some reason (changing business requirements, changing team structure, changin call patterns, changing functional requirements, changing costs, etc) we need to change the boundaries of these microservices, i.e move some functionality from one microservice to another, or merge services or split services,  it amounts to a re-architecture, or in the least, a huge code rewrite. This has been our modus-operandi to date. This is unfortunate. It would be ideal to be able to first think functionally, divide the end-to-2nd call graph into the right set of logical modules which coordinate with each other to solve the business problem, launch with a simple system design with a small number of services, and then, as requirements evolve and circumstances change, move around the logical modules among microservices without a major re-architecture, and critically, with close-to-zero changes to the functional code.

All code of vajram-lang that we have seen above talks about business logic executing within a common application runtime. But there are three features of the language which makes it well placed to solve the above problem \- the separation of control plane and data plane for dependency declaration, reactive execution of data plane logics, and the ability to add dynamic metadata to call graph edges without code changes.

The fact that inter-vajram dependencies are declared in the control plane of the vajram in a declarative, non-procedural manner means that no data plane code of the vajram is blocked on the completion of a dependency. All logics are executed reactively. These features in conjunction give us the ability to hide the whereabouts of the dependent vajram. The code of this vajram remains agnostic of whether the dependency is available locally or is available in a remote machine across a network. This allows us to re-distribute vajrams and compose executables (microservices) in a manner that suits us without a major rewrite of the code.   
One side-effect of moving vajrams from a common runtime to across a network is the new failure modes that this adds to the communication between the vajrams \- like network errors, timeouts etc. This is where the ability to dynamically add metadata to call graph edges proves useful. The application owner can annotate all such edges which span across the network with metadata like retryability, circuit breaker, timeouts, error code mappers, batchers etc without changing the vajram code.

Writing and distributing functionality as a vajram not only solves the problem stated above, but also unlocks new capabilities. We can now build a vajram call graph that spans the complete end to end call path \- from incoming user request to the final DB call(s) \- irrespective of the number of network hops in between. This exhaustive call graph can give functional insights (for debugging functional issues) and performance metrics (for debugging performance issues) at a granularity and detail which was simply unheard of before \- the utility of such a debugging tool cannot be overstated. 

### Async Orchestration

Async orchestration is the problem of orchestrating a call graph where some nodes in the call graph might take extremely long to complete. In comparison to synchronous orchestration (the original problem that Krystal solves) where nodes which make network calls might take a few seconds at max, nodes in asynchronous call graphs might take anywhere between a few minutes to years even, to complete. How do we write business logic for such a call graph? There are many programming models proposed for writing such code, but all of these are very different from the code that we write on the synchronous side of the world. The new nuance which gets added due to asynchronous call graphs is state persistence. When we call a module which might take a really long time to complete, we cannot just reactively wait for its response in the current VM (like we did before). Instead, we need to persist the complete state of the application in a DB, host a callback API endpoint which is sent to the delayable module, wait for and receive a callback at that endpoint, restore the state of the application from persistence and then resume execution with the response received in the callback. This complicates the code of asynchronous orchestration logic. We end up adding DB calls, persistent queue calls, choosing what to persist and what not to, idempotency caches, and so on…

vajram-lang has all the right primitives to allow us to write asynchronous call graphs as well, with much less ceremony. To support synchronous call graphs where nodes can make network calls which can take a few seconds, vajram-lang supports "delegating logics" which are denoted by `~{}`. To support long running asynchronous  jobs (like waiting for a human response), we add the support of "delayable logics" with the `~~` (later) operator. Unlike a delegating output logic which returns soon, a delayable output logic might not immediately return any value. Instead, the callback endpoint receives the callback which is translated to a message which provides the actual output value. This feature of vajram-lang where there is a native understanding of non-delegating, delegating and delayable logics gives us the power to localize delayability to a single vajram. The other vajrams which directly or indirectly depend on this vajram need not know that this vajram may take weeks to complete. Since the complete runtime is reactive and based on message-passing, and because the business logic in vajrams is modelled as stateless logics, the runtime doesn't care where the "response message" comes from. It may come from a local vajram or from a remote API call. But for this to work, we need to solve the persistence problem. Here is where the "facets" feature comes into play. Because facets are immutable type safe states of vajrams, when the call graph encounters a delayable vajram, all currently active/pending vajrams' state can be summarized by just the facet values of the vajrams. All we have to make sure is that the facets are serializable \- we do not need to change the programming model. This declarative definition of inputs and dependencies in the control plane allows the runtime to deterministically figure out the complete state of all the vajrams of the call graph without developers having to write DAO, DALs and transformers. This reduces boilerplate immensely. Not only this, vajram-lang allows us to do better. Facets allows the runtime to persist *all* the state. But that is not optimal. Vajram-lang allows for optimal persistence. This is possible because the various resolver logics and output logics consume facets and produce facets, and this inter-facet dependency is visible to the runtime. So when a delayable vajram is encountered, the runtime can infer which subset of the facets will be needed when the application is reloaded and resumed. If a facet has been used to compute other facets and there is no other yet un-computed facet which needs the facet, then persisting that facet is unnecessary. Most importantly, the runtime can create this set of actually needed facets without any further code to be written by the developer.

### State Machine

There are many ways to describe a vajram. One of them is that a vajram is a grouping of closely related, interdependent functions/logics along with associated monotonically-immutable state (you can set the value of a facet via a logic, but you cannot change an already set value). The other way is to see a vajram as a mini orchestrator which is orchestrating the various logics to compute facets and eventually to compute the output of the vajram

But we can also see a vajram as a state machine. And the complete vajram call graph is a set of inter-operating, interdependent state machines. The state of the state machine is represented by the data of the facets of the vajram. If a vajram has `N` facets, each with `M` possible values (including `nil`), then the vajram can theoretically be in a maximum of `M^N` states depending on the business logic of the vajram. Each logic can be seen as a transition from one state to another. So all we have to do to expose a vajram as a state machine is to annotate some facets as states. Let us look at a simple example of a linear state machine for ticket reservation which has these states   
1\. ticket selected   
2\. ticket reserved   
3\. payment initiated   
4\. payment confirmed   
5\. tickets booked.   
This how the linear state machine vajram might look like

**@LinearStateMachine**  
**bool** bookTickets(**@input** Event event, **@input int** count, **@input** userId){  
  TicketSelectionResponse? selectedTickets \= selectTickets(forEvent \= event, number \= count);  
  **@State @field**  
  **bool** ticketsSelected \= selectedTickets?valuePresent();

  TicketReservationResponse? reservedTickets \= reserveTickets(tickets \= selectedTickets.tickets());  
  **@State @field**  
  **bool** ticketsReserved \= reservedTickets?valuePresent();

  TicketPrice? ticketPrice \= getPrice(tickets \= reservedTickets.tickets());

  PaymentInit? paymentInit \= initPayment(  
                                 user \= userId,   
                                 amount \= ticketPrice.getPrice());  
  **@State @field**  
  **bool** paymentInitiated \= paymentInit?valuePresent();

  PaymentConfirmation? paymentConfirmation \= confirmPayment(  
                                 paymentId \= paymentInit.id());  
  **@State @field**  
  **bool** paymentConfirmed \= paymentConfirmation?valuePresent();

  TicketBooking ticketBooking \= confirmTickets(  
                                 tickets \= ticketsReserved.tickets(),  
                                 paymentId \= paymentConfirmation.paymentId());

  **@State @output**  
  {  
    ticketBooking.isSuccessful()  
  }  
}

(A small note on `@field`s: We have seen three types of facets in vajrams \- `@input`s, `@dep`endencies, and `@output`. Here we introduce a new kind of facet called a `@field`. A field is similar to a dependency \- it is immutable-after-set, and defined in the control plane; it is different in that it doesn't invoke another vajram, but is instead computed by a (non-delegated) data plane logic (a field computer). `@field`s are also useful in asynchronous orchestration code to compute intermediate data representations.)

Generally, the value returned by the output logic (along with input facets) forms the client-facing contract of a vajram while the. But in this case, by being annotated by `@LinearStateMachine` and by annotating facets with `@State`, the vajram developer is opting-in to exposing these facets as states which are part of the contract of the vajram. What is more, the developer doesn't even have to provide the order of the states (like `@State(1)`, `@State(2)` etc..), since the inter-facet dependencies are part of the structure of the vajram (thanks to them being defined in the control plane), the runtime can infer the state order and provide it to the vajram's clients. Also, we can add build time checks that the inter-state dependencies are actually linear, etc. There is no fundamentally new programming paradigm we introduced to unlock the capability of modelling state machines. The same vajram is able to function as one. This is again due to the generality of the way vajrams are modelled \- type-safe immutable-after-set facets which are tied among each other into a DAG by logics which consume some facets and compute a facet.

### SIMD

[SIMD](https://en.wikipedia.org/wiki/Single_instruction,_multiple_data), or Single Instruction Multiple Data is a computation model where a single instruction is executed on multiple values. For example, adding 1 to each number in an array of numbers. In presence of such computations it is beneficial to offload computation to processors like GPU or via special CPU instructions which have the ability to execute such instructions parallelly at hardware level. This allows programs to achieve significant performance improvements depending on the number of channels available. Java, for example, is planning to introduce [new Vector APIs](https://openjdk.org/jeps/460) to write logic which needs to be optimized by the SIMD pattern. Let's look at how this new API looks. First a simple `for` loop which does scalar computation :

| void scalarComputation(float\[\] a, float\[\] b, float\[\] c) {  for (int i \= 0; i \< a.length; i++) {     c\[i\] \= (a\[i\] \* a\[i\] \+ b\[i\] \* b\[i\]) \* \-1.0f;  }} |
| :---- |

This code takes two arrays of the same size and computes a third array where each element of the third array is the negation of the sum of squares of the other two elements. The corresponding vector API code looks like this:

| static final VectorSpecies\<Float\> SPECIES \= FloatVector.SPECIES\_PREFERRED;void vectorComputation(float\[\] a, float\[\] b, float\[\] c) {  int i \= 0;  int upperBound \= SPECIES.loopBound(a.length);  for (; i \< upperBound; i \+= SPECIES.length()) {    // FloatVector va, vb, vc;    var va \= FloatVector.fromArray(SPECIES, a, i);    var vb \= FloatVector.fromArray(SPECIES, b, i);    var vc \= va.mul(va).add(vb.mul(vb)).neg();    vc.intoArray(c, i);  }  for (; i \< a.length; i++) {    c\[i\] \= (a\[i\] \* a\[i\] \+ b\[i\] \* b\[i\]) \* \-1.0f;  }} |
| :---- |

The vajram-lang code without SIMD optimization looks almost exactly like the scalar java code, the only difference being that the vajram returns a new array rather than taking the array as input:  
**float**\[\] computeForArray(**float**\[\] a, **float**\[\] b){{  
  **float**\[\] c \= **new float**\[a.length\];  
  **for** (**int** i \= a.indices()) {  
    c\[i\] \= (a\[i\] \* a\[i\] \+ b\[i\] \* b\[i\]) \* \-1.0f;  
  }  
  c  
}}

To optimize this code to use SIMD optimization, we first decompose this into two vajrams \- one to perform the core computation (`a*a + b*b * -1.0`) and one to pass the array values to this vajram:

**float** computeValue(**float** a, **float** b){{  
   (a\*a \+ b\*b) \* \-1.0f  
}}

**float**\[\] computeForArray(**float**\[\] a, **float**\[\] b){  
  Fanout\<**float**\> computedValue \= computeValue(a,b \=\*  
      **for**(i : a.indices()){  
        **yield** a\[i\], b\[i\]  
      }  
  )

  {  
    computedValue.values().toArray()  
  }  
}

Here we are using the fanout dependency capability to delegate the core computation to a different vajram. In the invocation of `computeValue` vajram, we use the `=*` operator to perform a fanout, and `yield` values from inside a `for` loop to create an iterable of tuples which are assigned to the inputs `a` and `b` of `computeValue` which will be called `N` times where `N` is the iterable item count. The next step is to optimize the `computeValue` vajram \- we do this by moving the mathematical computations from the data plane to the control plane \- we'll see how this gives the platform the ability to optimize.

**float** computeValue(**float** a, **float** b){  
 **float** aSquare \= a \* a;  
 **float** bSquare \= b \* b;  
 **float** sum \= aSquare \+ bSquare;  
 **@output**  
 **float** result \= sum \* \-1.0;  
}

This change might seem trivial and verbose. We are doing the exact same thing as before, but storing each intermediate operation as a facet. How does this help? The key is the fact that the computation is being performed in the control plane. Here, imagine the two-operand '`*`' operator as a language native vajram taking two operands as input. Also, imagine it has added the `#batch` tag to the input:  
*// Invoked by the language runtime when two numbers are multiplied*  
T binaryMultiply\<T\>(**\#batch** T operand1, **\#batch** T operand2){  
 {  
   **list**\<**\#batch**\> \= **\#batch**.batches();  
   *// language native implementation of*  
   *// SIMD-optimized multiplication logic*  
 }  
}

As you might predict, this changes things. The `binaryMultiply` vajram takes over from the traditional multiplication operator(`*`). When executing the call path `computeForArray:computedValue->computeValue:aSquared->binaryMultiply`, the language runtime knows that the `computedValue` facet in `computeForArray` is a fanout dependency \- which means `computeValue` will be called multiple times which inturn means `binaryMultiply` will be called multiple times. With this knowledge, the runtime can group all the multiplications into a single batch call (using the batching capability we spoke of above) and let the vajram perform the computation to the best of its capabilities. It might have the intelligence to look at the size of the batch and decide whether to perform a traditional multiplication or to perform a SIMD operation. And that's it\! The code is SIMD-optimized.

Now that we have understood the functioning of the code, we can simplify the `computeValue` vajram a bit. We don't need one facet each for `aSquared`, `bSquared` and so on:  
**float** computeValue(**float** a, **float** b){  
 **@output**  
 **float** result \= a\*a \+ b\*b \* (-1.0);  
}

The language runtime can parse the mathematical formula and infer the syntax tree, and make the same optimization as above. Not only that, the language runtime knows that `a*a` and `b*b` are independent of each other (the same way it's able to deduce that `aSquared` and `bSquared` are independent of each other since they do not consume each other's outputs directly or indirectly). This means that it can not only parallelize the batched `a*a` calls into a single vector operation, but also combine the `a*a` and `b*b` into a single vector operation\! Depending on the batch size and the number of lanes in the hardware architecture, this can be a huge win. So, here is the final concise code:  
**float**\[\] computeForArray(**float**\[\] a, **float**\[\] b){  
  computeValue(a,b \=\* {  
    **for**(**int** i : a.indices()){  
      **yield** a\[i\], b\[i\]  
    }  
  }).values()  
}

**float** computeValue(**float** a, **float** b){  
   a\*a \+ b\*b \* (-1.0);  
}  
The original vajram with the scalar implementation had 165 characters of code. This SIMD optimized code has 199 \- a 21% increase. Compare this to the java version \- the original scalar implementation had 139 characters, while the vectorized code using the new vector API has 536 characters \- a 385% increase. The increase in the amount of code is one thing, but the most important difference between the before and after is not the length of the code, but the new concepts introduced. To vectorize code reliably in traditional programming languages, developers generally have to learn new programming concepts and APIs. New ways to do age old things. Things as simple as multiplication `a*a` become `va.mul(va)`. This change in programming model should not be needed, ideally. As you can see in the vajram code, there is no new concept we have introduced to achieve vectorization, and there is nothing in the code related to the concept of vectors. We are able to achieve this because of the fundamental feature of vajram-lang \- the control plane. If we look at the original scalar java code, one might wonder why the java compiler/runtime cannot just auto-vectorize the code itself. The answer is that, in many cases, it can. But it is not always possible. Quoting from the [Vector API JEP](https://openjdk.org/jeps/460):   
"*In general, even after decades of research — especially for FORTRAN and C array loops — it seems that auto-vectorization of scalar code is not a reliable tactic for optimizing ad-hoc user-written loops unless the user pays unusually careful attention to unwritten contracts about exactly which loops a compiler is prepared to auto-vectorize. It is too easy to write a loop that fails to auto-vectorize…*"    
This "brittleness" of traditional loops comes from the fact that developers can do almost anything in the loops. This is where the restrictions of the vajram-lang control plane come into play. By placing strict restrictions on the kind of code that the control plan can contain, the language can reliably vectorize (sometimes maybe even better than a developer could do \- through dependency analysis in the syntax tree of the formula, as we discussed). This restriction in the control plane also allows the language to control the inter-vajram dispatch method as needed. When a method is called inside a traditional for-loop, the language runtime is forced to dispatch multiple calls to the method one at a time \- one per iteration \- at least this is what the semantics of the for loop dictates. But the control plane of vajram-lang allows to call another vajram using the fanout operator `=*` which is a replacement for a for loop and allows for parallel or sequential dispatch, as needed. To solve the vectorization problem, the restrictive control plane works well with the ability of the runtime to statically analyze the vajram call graph (for presence of fanouts) and the language native batching feature. 

### Reactive Streams

Synchronous orchestration and asynchronous orchestration have traditionally had extremely different programming models as we have discussed above. Another example of a paradigm which has traditionally demanded a completely different programming model is reactive streams. The paradigm of Reactive streams is based on the [reactive manifesto](https://www.reactivemanifesto.org/) which documents the runtime characteristics that a system needs to have to be called a reactive system. The manifesto doesn't profess any particular programming model or developer experience. The [Flow](https://www.reactive-streams.org/) standard library since JDK9 provides base interfaces for the reactive streams programming model. This again is a new programming model which developers have to learn to write code which can stream events reactively. 

The vajram-lang runtime is reactive by nature. Maybe the same programming model can be extended to the reactive streams paradigm as well? Let's take a simple java example written with [io.reactivex.rxjava3](https://github.com/ReactiveX/RxJava):

| private static Flowable\<Integer\> streamNumbers() {  return Flowable.*fromArray*(1, 2, 3, 4, 5, 6, 7, 8); } private static int addOne(int i) { return i \+ 1; } @Test void printNumbers() {  Flowable\<Integer\> number \= *streamNumbers*();  Flowable\<Object\> plusOne \= number.map(i \-\> *addOne*(i));  plusOne.subscribe(      System.*out*::println,      e \-\> System.*err*.println("Ouch\!"),      () \-\> System.*out*.println("Done\!")); } |
| :---- |

This code streams a few numbers, adds 1 to each, and then prints each. When the stream is closed, "Done\!" is printed. When an error is encountered, "Ouch\!" is printed. Same streaming code in vajram-lang:

**\#stream int** streamNumbers(){{ \[1,2,3,4,5,6,7,8\] }}

**int** addOne(**int** input){{ input+1 }}

**void** printNumbers(**@inject** ConsoleWriter writer){  
 **\#stream int**? number \= streamNumbers();  
 **\#stream int**? plusOne \= addOne(number);

 {  
   **switch**(plusOne){  
     **int** \_ : writer.Out.println(\_);  
     **err** \_ : writer.Err.println("Ouch\!");  
     **nil**   : writer.Out.println("Done\!");  
   }  
 }  
}  
The number of new concepts introduced in the java code: 5 (`Flowable`, `map`, `subscribe`, `onNext`, `onComplete`, `onError`). The number of new concepts introduced in vajram-lang: 1 (`#stream`). The `#stream` tag allows for the compiler to do the heavy lifting. The `#stream` tag on the `streamNumbers` vajram tells the compiler that the vajram returns a stream of `int`s. It also tells the compiler that the output logic of the vajram may return an iterable \- the compiler plugin will do the conversion from iterable to stream. The developer doesn't have to do anything, or even learn a new data type to represent the stream. The `#stream` tag on the `number` facet tells the compiler that the multiple numbers are going to stream through this facet. The `#stream` tag on the `plusOne` facet tells the compiler that `addOne` should be invoked multiple times depending on the number of items in the `number` stream, and the result is to be streamed through the `plusOne` facet. Finally the runtime calls the output logic of `printNumbers` as many times as there are items in the `plusOne` stream. The beauty of the `#stream` tag is that the output logic can consume the number as if it were just a normal `int`. There is no need for developers to learn new APIs to consume values from a stream (like `map`, `subscribe`) etc. And finally, the "errability" feature spares the developer from learning about new api params (like onNext, onComplete, onError etc). Instead, a simple switch statement allows the dev to access the current element in the stream. If the `plusOne` facet has `nil` it means the stream is closed. If it has an `err`or, then the stream has errored out. 

It is important to note here that the above is in no way an exhaustive reactive-streams implementation. There is a lot more to solve to make vajram-lang fully compatible with the reactive streams specification. Not only that, we have only shown the applicability of vajram-lang's grammar to the reactive streams paradigm. The runtime is a different ball-game altogether. The language runtime might need to add a lot more custom capabilities to execute reactive streams. All these challenges notwithstanding, the hope is that this section at least shows the generality of the Krystalline programming model.

### SAGAs

[The Saga pattern](https://www.cs.cornell.edu/andru/cs711/2002fa/reading/sagas.pdf) allows us to model business workflows involving multiple disjointed transactions. To design a saga, developers have to register a compensating transaction for each transaction which makes up the saga. The Saga Manager keeps track of each transaction and when one transaction fails in the list of transactions making up the saga, the compensating transactions of each of the other successful transactions is executed to undo their effect on data stores and bring the complete system to the original state before the saga was executed.  The Krystal programming model allows us to model these with ease and minimal developer overhead.

## A Final Note

The generality with which we are able to extend the scope of vajram-lang from its initial limited scope is heartening. While most of what has been said in this section is theoretical, it does give us some confidence that the programming model and the tenets of Krystalline programming are not arbitrary. There is good reason to believe that the abstractions and concepts that the Krystalline programming paradigm is based on, would most probably stand the test of time. With this confidence that we are on the right track, we can go ahead with the implementation.

# Vajram Java SDK

Conceiving, designing and building a new programming language from scratch, however exciting it might seem, is no trivial task. The amount of effort needed to build a new programming language is not even the biggest concern. Evangelizing, educating and then driving adoption of this new programming language across a company the size of Flipkart would be a humongous project. We cannot take an all or nothing approach. We need to find an incremental in-between milestone where we might not get all the goodness that vajram-lang can give us, but at least some of it.

In this spirit, we started the Krystal project as a Java-based Software Development Kit (SDK) rather than an altogether new programming language.  This, we call the vajram-java-sdk. The vajram java sdk allows developers to write vajrams in the Java(17+) programming language which runs in the JVM. All the basic concepts and abstractions introduced in vajram-lang remain the same \- vajrams are the smallest units of work, they have immutable-after-set type-safe facets (inputs, injections, dependencies), these facets can be mandatory or optional, dependencies can fanout, there is control plane and data plane separation, dependency resolvers compute inputs to dependencies, and output logic computes the output of the vajrams, platform developers can extend platform capabilities with output logic decorators, facets and vajrams can be annotated, the sdk supports batching as a capability natively, and so on.. 

This adherence to the tenets of Krystalline programming gives us much of the goodness that we have aimed to deliver in the design of vajram-lang. The thing we miss out on is the brevity of vajram-lang. Given that the grammar of the java language was not designed to be compatible with Krystalline programming, it takes a bit more code to achieve the same thing that vajram-lang is able to do in a few lines of code. To mitigate this, vajram-java-sdk relies heavily on code-generation so that the vajram is executed in a performant way (without having to resort to reflection, for example) by the framework's runtime \-  called Krystex (from **Kryst**al **Ex**ecutor). The reliance on code-generation reduces unnecessary boilerplate to a huge extent and allows the framework to adapt to the developers' needs, but this needs some innovation and configuration of the IDE and build systems to make sure the code generation is being performed seamlessly.

Let's take a look at the `isProductAvailable`, `getProductDetails` and `getAvailableProductDetails` vajrams written using the vajram-java-sdk to get a better understanding.

| @VajramDefabstract class IsProductAvailable extends IOVajram\<Boolean\> {  class \_Facets {    @Input String productId;    @Inject AvailabilityDB availabilityDB;  }  @Output  static CompletableFuture\<Boolean\> isProductAvailable(      IsProductAvailableFacets facets) {    return facets     .availabilityDB()     .isProductAvailableAsync(facets.productId());  }} |
| :---- |

| @VajramDefabstract class GetProductDetails extends IOVajram\<ProductDetails\> { class \_Facets {   @Input String productId;   @Inject ProductDB productDB; }  @Output  static CompletableFuture\<ProductDetails\> getProductDetails(       String productId, ProductDB productDB) {    return productDB.getProductDetailsAsync(productId); }} |
| :---- |

| @VajramDefpublic abstract class GetAvailableProductDetails extends ComputeVajram\<ProductDetails\> {  class \_Facets {    @Input String productId;    @Dependency(onVajram \= IsProductAvailable.class)    Optional\<Boolean\> isAvailable;    @Dependency(onVajram \= GetProductDetails.class)    Optional\<ProductDetails\> productDetails;  }  @Resolve(depName \= isAvailable\_n,      depInputs \= IsProductAvailableRequest.productId\_n)  static String productIdForIsAvailable(String productId) {    return productId;  }  @Resolve(depName \= productDetails\_n,      depInputs \= IsProductAvailableRequest.productId\_n)  static SingleExecute\<String\> productIdForProductDetails(      String productId, boolean isAvailable) {   if (isAvailable) {     return SingleExecute.executeWith(productId);   } else {     return SingleExecute.skipExecution("Product is not available");   } }  @Output  static ProductDetails getAvailableProductDetails(       ProductDetails productDetails) {    return productDetails;  }} |
| :---- |

* Vajrams written using the vajram-java-sdk are abstract classes annotated with `@VajramDef` and either extend `ComputeVajram` (for non-delegated computation) or `IOVajram` (for delegated computation).   
* The `@input`, `@inject` and `@dep` facets are all declared in an inner class which must be named `_Facets`.  
* Dependency facets can specify the Vajram they depend on using the `@Dependency` annotation's `onVajram` param.  
* If the dependency is a fanout dependency, then the `canFanout` param of `@Dependency` must be set to true.  
* The dependency resolvers are defined using static methods annotated with `@Resolve`. The `depName` annotation param specifies the name of the dependency being resolved and the `depInputs` annotation param specifies which inputs of the dependency are being resolved by the method. The resolver method can have any name.  
* The resolver method can access the facets by specifying the data type and facet name of the facet.  
* The return type of the resolver matches the data type of the input being resolved for non-fanout dependencies  
* The return type of the resolver method must be a `Collection` of objects if the dependency is a fanout dependency.  
* If the dependency needs to be skipped in some scenarios, then the resolver method can return `SingleExecute.skipExecution("reason")` command (`MultiExecute.skipExecution()`) if this is a fanout resolver.

## Applications

The Vajram Java SDK has been deployed as part of the mAPI redesign (mAPI 2.0) \- details of which can be found [here](https://docs.google.com/document/d/1aGs3t79Cid8QlIVWSHSLmOERSx4PBVH6ytRH3UqOL_I/edit#heading=h.yk0u4g8bvegd)