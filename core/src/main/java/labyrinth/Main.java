package labyrinth;

public class Main {

	public static void main(String[] args) {
		System.out.println("test");
		System.out.println("test2");
		System.out.println(Main.class.getClassLoader().getResourceAsStream("res1"));
	}
}