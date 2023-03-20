<h1 align="center">
  <a href="https://github.com/nampython/IoC-Container">
    <img src="slug/HappyFace.svg" alt="Logo" width="125" height="125">
  </a>
</h1>

<div align="center">
  Amazing Project - Feel free to learn!
  <br />
  <br />
  <a href="https://github.com/nampython/IoC-Container/issues/new?assignees=&labels=bug&template=bug_report.md&title=">Report a Bug</a>
  ·
  <a href="https://github.com/nampython/IoC-Container/issues/new?assignees=&labels=enhancement&template=feature_request.md&title=">Request a Feature</a>
  .
  <a href="https://github.com/nampython/IoC-Container/discussions">Ask a Question</a>
</div>




# IoC Container
As a curious and passionate software developer, I always wondered how Spring Core works. To better understand its inner workings, I created this project that mainly uses Java Reflection and supports most annotations like @Component, @Autowire, @Bean, @PostConstructor, and more. Manage instantiated Components by an ApplicationContext (reloading, updating, getting) Bean like Spring Core. Handle custom components and bean scopes by using proxies. Enrich Components with aspects. And  automatically resolve dependencies


## Table of Contents
* [General Information](#general-information)
* [Prerequisites](#prerequisites)
* [Installation & Getting Started](#prerequisites)
* [More info](#more-info)

## General Information
A library that replicates the functionality of the Spring Core framework. It supports most annotations such as @`Component`, `@Service`, `@Repository`, `@Configuration`, `@Autowire`, `@Qualifier`, `@PostConstruct`, `@PreDestroy`... In addition, there are 3 modes for a bean (SINGLETON, PROTOTYPE, PROXY). Building from scratch helps us understand how IoC Containers work under the hood.

It’s really hard and complicated to know how spring core works. Because it is composed of many modules and is produced by many people. I try to integrate many components into one and make it simple by following these steps:

- Finding and loading all classes available in the project and pushing them into a set collection. Pay attention to getting classes because classes in directory is different compared to classes in the directory
- Scanning and filtering the classes that are identified are components.
- Instantiate these components to become an instance.
- Made an Application Context interface. As we know Spring has an interface applicationContext to retrieve all beans. I also made an applicationContext interface to retrieve it from getting, reloading, and updating beans.


## Prerequisites
- **Dealing with class:** [https://www.tutorialspoint.com/java/lang/java_lang_class.htm](https://www.tutorialspoint.com/java/lang/java_lang_class.htm)
- **Work with files**: [https://docs.oracle.com/javase/7/docs/api/java/io/File.html](https://docs.oracle.com/javase/7/docs/api/java/io/File.html)
- **Java reflection (Method, Annotation, Constructor…)**: [https://www.oracle.com/technical-resources/articles/java/javareflection.html](https://www.oracle.com/technical-resources/articles/java/javareflection.html)
- **Basic Spring Core framework**: Understanding how spring core work and know several basic annotations like @Bean, @Service, @Postconstruct,  @AfterDestroy, @Autowired…:[https://www.baeldung.com/spring-core-annotations](https://www.baeldung.com/spring-core-annotations)
- **Design Pattern:**
    - Singleton
    - Prototype
    - Builder
    - Proxy
    
## Installation & Getting Started
  - Run **'mvn clean install'** and get the dependency in the m2 folder.
    - Just import the dependency into your project.
        ```xml
        <dependency>
          <groupId>org.ioc</groupId>
          <artifactId>IOC-Container</artifactId>
          <version>1.0-SNAPSHOT</version>
        </dependency>
        ```
        
- In your main method call '`InitApplicationContext`.run(YourStartupClass.class) and annotate your startup class with @Component
    
    ```java
    @Component
    public class ApplicationEntryPoint {
        public static void main(String[] args) {
            InitApplicationContext.run(ApplicationEntryPoint.class, config);
     }
    ```
    

You can also run the app with 'InitApplicationContext.run(YourStartupClass.class, new MagicConfiguration()). 

```java
Configuration config = new Configuration()
                .instantiations()
                .addDependencyResolver(new StringConfigProducer())
                .addDependencyResolver(new StringConfigProducer2())
                .and()
                .scanning() // add custom Componetn and Bean
                .addComponentAnnotation(CustomServiceAnnotation.class)
                .addBeanAnnotation(CustomBeanAnnotation.class)
                .and();
```

**Below are the supported annotations in this library:** 

- `@Component` - `@Service` - `@Configuration`  - `@Repository`
    - Actually, We can use a configuration class to provide custom annotations that can act like @Bean and @Component.
- `@Bean` - Specify the bean-producing method.
- `@Scope` - Specify the scope of the component. **SINGLETON**, **PROTOTYPE,** or **PROXY**.
- `@Autowired` - Inject an instance in the constructor or field of a object
- `@PostConstruct` - Specify a method that will be executed after the component has been created.
- `@PreDestroy` - Specify a method that will be executed just before the component has been disposed.
- `@StartUp` - Specify the startup method for the app.
- `@AliasFor` - Use this annotation to integrate your own annotations with InitApplicationContext. AliasFor works with Autowired, NamedInstance, Nullable, PostConstruct, PreDestroy, Qualifier.
- `@NamedInstance` - Specify the name of the component/ bean.
- `@Nullable` - required dependency can be null.
- `@Qualifier` - Specify the name of the dependency that you are requiring.


## More info
If you are having trouble running the app, contact me at quangnam130520@gmail.com
