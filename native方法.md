# Native方法

因为看源码的过程中，遇到过不少native方法，所以这里记录下遇到的情况。

## intern

​	String.intern()是一个Native方法，底层调用C++的 StringTable::intern方法实现。当通过语句str.intern()调用intern()方法后，JVM 就会在当前类的常量池中查找是否存在与str等值的String，若存在，则直接返回常量池中相应Strnig的引用；若不存在，则会在常量池中创建一个等值的String，然后返回这个String在常量池中的引用。

```java
public native String intern();
```

实际情况：

```java
    @Test
    void contextLoads() {
        String s = new String("1");//在堆中创建的实际变量地址
        String s3=s.intern();//获取到的引用地址
        String s2 ="1";//从常量池中获取到的引用地址
        System.out.println(s == s2);//false
        System.out.println(s3 == s);//false
        System.out.println(s3 == s2);//true
    }
```

