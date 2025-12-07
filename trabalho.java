import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;


// caio medeiros - 22306939

class LogWriter {
    private static final String FILE_NAME = "log_buffer.txt";

    public static synchronized void write(String message) {
        try (FileWriter writer = new FileWriter(FILE_NAME, true)) {
            writer.write(message + "\n");
        } catch (IOException e) {
            System.out.println("Erro ao escrever no arquivo de log: " + e.getMessage());
        }
    }
}


class Buffer {
    private final Queue<Integer> fila = new LinkedList<>();
    private final int capacidade = 7;

    private final Semaphore semaforoEspacos; 
    private final Semaphore semaforoItens;   
    private final ReentrantLock lock;        

    public Buffer() {
        this.semaforoEspacos = new Semaphore(capacidade, true);
        this.semaforoItens = new Semaphore(0, true);
        this.lock = new ReentrantLock(true);
    }

    
    public void produzir(int item, String nomeProdutor) throws InterruptedException {
        semaforoEspacos.acquire(); 
        lock.lock();
        try {
            fila.add(item);
            int espacosDisponiveis = capacidade - fila.size();
            String mensagem = nomeProdutor + " - Inserido um item no buffer – espaços disponíveis: " + espacosDisponiveis;
            System.out.println(mensagem);
            LogWriter.write(mensagem);
        } finally {
            lock.unlock();
            semaforoItens.release(); 
        }
    }

    
    public int consumir(String nomeConsumidor) throws InterruptedException {
        semaforoItens.acquire(); 
        lock.lock();
        try {
            int item = fila.remove();
            int espacosDisponiveis = capacidade - fila.size();
            String mensagem = nomeConsumidor + " - Consumido um item do buffer – espaços disponíveis: " + espacosDisponiveis;
            System.out.println(mensagem);
            LogWriter.write(mensagem);
            return item;
        } finally {
            lock.unlock();
            semaforoEspacos.release(); 
        }
    }
}


class Produtor extends Thread {
    private final Buffer buffer;
    private final int id;

    public Produtor(Buffer buffer, int id) {
        this.buffer = buffer;
        this.id = id;
    }

    @Override
    public void run() {
        for (int i = 1; i <= 15; i++) {
            try {
                int item = (int) (Math.random() * 100);
                buffer.produzir(item, "Produtor " + id);
                Thread.sleep((int) (Math.random() * 400));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}


class Consumidor extends Thread {
    private final Buffer buffer;
    private final int id;

    public Consumidor(Buffer buffer, int id) {
        this.buffer = buffer;
        this.id = id;
    }

    @Override
    public void run() {
        for (int i = 1; i <= 12; i++) {
            try {
                buffer.consumir("Consumidor " + id);
                Thread.sleep((int) (Math.random() * 500));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}


public class Main {
    public static void main(String[] args) {
        Buffer buffer = new Buffer();

        Produtor p1 = new Produtor(buffer, 1);
        Consumidor c1 = new Consumidor(buffer, 1);

        p1.start();
        c1.start();

        try {
            p1.join();
            c1.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Execução finalizada. Verifique o arquivo log_buffer.txt.");
    }
}
