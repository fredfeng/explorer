package edu.utexas.cflexamples;

public class Driver {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		Fruit a = new Apple();
		Fruit b = new Peach();
		

//		Class clazz = Class.forName("edu.utexas.cflexamples.Apple");
//		Fruit c = (Fruit)clazz.newInstance();

		//String ap = "apple";
		a.sayHello("hi");
		Fruit pe = a.myfoo();
		pe.myfoo();
		b.sayHello("fok");
	}

}
