package com.aspire.cmppsgw.startup;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.AccessControlException;
import java.util.Random;

import org.slf4j.Logger;

import com.aspire.cmppsgw.MonitorJob;
import com.aspire.cmppsgw.util.LogAgent;


/**
 * main server
 */
public class Server {
	Logger logger = LogAgent.systemInfoLogger;
	/**
	 * �����������
	 */
	private Random random = null;

	/**
	 * shutdown����
	 */
	private String shutdown = null;

	/**
	 * �ȴ� shutdown ����Ķ˿ں�
	 */
	private int port;

	/**
	 * flag
	 */
	private boolean running = true;

	
	/**
	 * ���캯��
	 * 
	 * @param shutdown
	 *            ��������ָֹ��
	 * @param port
	 *            ��������ָֹ������˿�
	 * @param scanner
	 *            ������ɨ����
	 */
	public Server(String shutdown, int port) {
		this.shutdown = shutdown;
		this.port = port;
	
	}

	public Random getRandom() {
		return random;
	}

	public String getShutdown() {
		return shutdown;
	}

	public int getPort() {
		return port;
	}

	public boolean isRunning() {
		return running;
	}

	public void setRandom(Random random) {
		this.random = random;
	}

	public void setRunning(boolean running) {
		this.running = running;
	}

	public void load() throws Exception {
		start(); // ��������
		await(); // �ȴ���ֹ��������
		stop(); // ��ֹ����
	}

	private void start() throws Exception {

		if (checkStarted()) {
			logger.info("Server has been started at [port:" + port
					+ "] ,System exit!!! !!!");

			// if server has been start then exit;
			System.exit(1);
		}
		MonitorJob.getInstance().start();
		logger.info("Starting Server success ");

	}

	/**
	 * check if the system started,if started return true;
	 * 
	 * @return
	 */
	private boolean checkStarted() {
		boolean bRet = false;

		Socket socket = null;
		try {
			socket = new Socket("127.0.0.1", port);
			// if socket is not null then the system has been started
			if (null != socket) {
				bRet = true;
				return bRet;
			}
		} catch (IOException ioe)// not started
		{
			bRet = false;
		}

		return bRet;
	}

	/**
	 * �ȴ�shutdown���Ȼ��return
	 */
	public void await() {

		// ����һ�� Server Socket ���ȴ���Ϣ
		ServerSocket serverSocket = null;

		// ��֤һ������������һ������
		try {

			serverSocket = new ServerSocket(port, 1,
					InetAddress.getByName("127.0.0.1"));
		} catch (IOException e) {
			System.err.println("Server.start: create[" + port + "]: \n " + e);
			serverSocket = null;
			System.exit(1);
		}

		// ѭ���ȴ�һ�����ӣ������кϷ�������
		while (true) {

			// �ȴ���һ������
			Socket socket = null;
			InputStream stream = null;
			try {
				socket = serverSocket.accept();
				socket.setSoTimeout(10 * 1000); // 10��
				stream = socket.getInputStream();
			} catch (AccessControlException ace) {
				System.err.println("Server.accept security exception: "
						+ ace.getMessage() + "\n" + ace);
				continue;
			} catch (IOException e) {
				System.err.println("Server.await: accept: \n" + e);
				System.exit(1);
			}

			// ��Socket�ж�ȡһ���ַ�
			StringBuffer command = new StringBuffer();
			int expected = 1024; // Cut off to avoid DoS attack
			while (expected < shutdown.length()) {
				if (random == null)
					random = new Random(System.currentTimeMillis());
				expected += (random.nextInt() % 1024);
			}
			while (expected > 0) {
				int ch = -1;
				try {
					ch = stream.read();
				} catch (IOException e) {
					System.err.println("Server.await: read: \n" + e);
					ch = -1;
				}
				if (ch < 32) // Control character or EOF terminates loop
					break;
				command.append((char) ch);
				expected--;
			}

			// Close the socket now that we are done with it
			try {
				socket.close();
			} catch (IOException e) {
				;
			}

			// Match against our command string
			boolean match = command.toString().equals(shutdown);
			if (match) { // �Ƿ�ƥ��
				break; // ���ƥ�����˳�ѭ��
			} else {
				logger.error("Server.await: Invalid command '"
						+ command.toString() + "' received");
			}
		}

		// �ر� server socket ����return
		try {
			serverSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private void stop() {
		MonitorJob.getInstance().stopJob();
		logger.info("Stoping Server sucess");
		System.exit(1);
	}
}