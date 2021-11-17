package cn.zsp.spi;
import java.util.Iterator;
import java.util.ServiceLoader;
public class App {
     public static void main(String[] args) {

         ServiceLoader<Car> serviceLoader = ServiceLoader.load(Car.class);
         serviceLoader.forEach(Car::goBeijing);
         //看不懂？没关系，我写个简单的
         Iterator<Car> iterator = serviceLoader.iterator();
         while (iterator.hasNext()){
             iterator.next().goBeijing();
         }
     }
 }