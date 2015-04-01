import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;


public class Nodo extends Thread {

	private int id;
	private ServerSocket servSocket; //per il thread che fa da listener 
	private ArrayList<Integer> indirizziNodi;
	public CodaMessaggiRicevuti msgRicevuti; //qui ci metto i messaggi ricevuti sui vari socket, prima che vengano consumati dal thread principale di questo oggetto
	private boolean valid = true;
	public HashMap<Integer,ArrayList<String>> storicoMessaggiRicevutiPerCanali; //qui ci metto, per ciascun mittende, tutti i messaggi che mi sono arrivati e che ho consumato
	
	private boolean startSnapshot = false;
	private boolean firstMarker = true;
	
	//quando un nodo riceve il primo marker, tutti i messaggi contenuti in storicoMessaggiPerCanali vengono salvati nello stato del nodo, quindi
	//vengono tolti da storicoMessaggiPerCanali e messi in
	public ArrayList<String> messaggiStatoNodoRicevuti;
	public ArrayList<String> messaggiStatoNodoInviati;
	//mentre tutti i messaggi successivi che arriveranno, verranno lasciati in storicoMessaggiPerCanali, e rappresentano i messaggi contenuti nel canale (nel momento dello snapshot)
	
	public Nodo(int Id,ArrayList<Integer> indirizziNodi )
	{
		this.id = Id;
		this.indirizziNodi = indirizziNodi;
		int miaPorta = (int) (10000+Math.random() *10000 ); //genero randomicamente la porta su cui binderò il listener thread
		indirizziNodi.add (new Integer(miaPorta)); //salvo la porta nella struttura dati generale
		
		try 
		{
			servSocket = new ServerSocket(miaPorta,10000); //creo socket su porta random (questo socket sarà usato dal thread listener)
//			System.out.println("CREATO SOCKET SU PORTA "+miaPorta);
			
			msgRicevuti = new CodaMessaggiRicevuti();
			storicoMessaggiRicevutiPerCanali = new HashMap<Integer,ArrayList<String>>();
			messaggiStatoNodoRicevuti = new ArrayList<String>();
			messaggiStatoNodoInviati = new ArrayList<String>();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
			valid = false;
		} 
	}
	
	public boolean isValido()
	{
		return valid;
	}
	
	public int getMioId()
	{
		return id;
	}
	
	public void startSnapshot()
	{
		startSnapshot = true;
	}
	
	@Override
	public void run() {
		ThreadListenerPerSocket listener = new ThreadListenerPerSocket();
		listener.start();
		
		for(;;)
		{
			
			if(startSnapshot)
			{
				startSnapshot = false;
				
				salvaMessaggiInviatiRicevutiComeStatoNodo();
				inviaMarkerATuttiNodi();
				
			}
		

			String msgRicevuto = msgRicevuti.pop();
			
			if(null != msgRicevuto)
			{
				
				if(msgRicevuto.equals("marker"))
				{ 	
					//in realtà abbiamo ricevuto il marker
					if(firstMarker)
					{
						firstMarker = false;
						salvaMessaggiInviatiRicevutiComeStatoNodo();
						inviaMarkerATuttiNodi();
					}
					else
					{
						//lo stato dei canali sono i messaggi che attualmente si trovano in storicoMessaggi...
						return; //lo snapshot è terminato, quindi il thread termina. In storicoMessaggiRicevutiNodo ci sono i messaggi relativi ai canali
						//mentre in messaggiStatoInviati e messaggiStatoRicevuti ci sono i messaggi che compongono lo stato del nodo
					}
					
				}
				else //abbiamo ricevuto un messaggio normale
				{
					//ottengo id di chi ha inviato
					String[] splitted = msgRicevuto.split("-");
					int idMittente = Integer.parseInt(splitted[0]);
					
					//aggiungo nello storico dei messaggi di provenienza da nodo con quell'id
					if(!storicoMessaggiRicevutiPerCanali.containsKey(idMittente)) //se non esiste ancora l'array list nell'hash map associata a quel nodo di provenienza, lo creo
					{ 
						storicoMessaggiRicevutiPerCanali.put(idMittente, new ArrayList<String>());
					}
					
					storicoMessaggiRicevutiPerCanali.get(idMittente).add(msgRicevuto);
					

//					System.out.println("THREAD CON ID:"+id+" RICEVUTO MSG DA "+idMittente+" : "+msgRicevuto);

				}
								
			}
			else
			{ //non ho ricevuto messaggi, decido randomicamente se inviare messaggio a qualcuno degli altri nodi (scelto randomicamente)
				
			    double randN = Math.random()*1000;
			    if(randN > 750) //allora invia
			    {
			    	//ad un nodo random
			    	int idDest = (int)(Math.random() * indirizziNodi.size() ); //genero randomicamente indice del nodo destinatario (che poi è anche l'id)
			    	String msgToSend = this.id + "-" + idDest + "\n";
			    	ThreadScrittore tS = new ThreadScrittore(indirizziNodi.get(idDest).intValue(),msgToSend);
			    	tS.start();
			    	
			    } else
					try {
						sleep(5000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			}
		}
	
	}
	
	private void salvaMessaggiInviatiRicevutiComeStatoNodo()
	{
		//salva tutti i messaggi inviati e ricevuti, come facenti parte dello stato del nodo
		for(Integer idMittente : storicoMessaggiRicevutiPerCanali.keySet())
		{
			for(String msg : storicoMessaggiRicevutiPerCanali.get(idMittente))
			{
				messaggiStatoNodoRicevuti.add(msg);
			}
		}
		//gli inviati sono già stati inseriti sulla struttura messaggiStatoNodoInviati direttamente nella fase di invio
		
		//e svuoto gli storici 
		storicoMessaggiRicevutiPerCanali.clear();
		
	}
	
	private void inviaMarkerATuttiNodi()
	{
		//invia marker a tutti i nodi
		for(Integer portaNodo : indirizziNodi)
		{
			ThreadScrittore tS = new ThreadScrittore(portaNodo.intValue(),"marker"); 
			tS.start();
		}
	}
	
	
	private class CodaMessaggiRicevuti
	{
		ArrayList<String> messaggi = new ArrayList<String>();
		
		public synchronized void push(String msg)
		{
			messaggi.add(msg);
		}
		
		public synchronized String pop()
		{   
			if(messaggi.isEmpty())
				return null;
			String msg = messaggi.get(messaggi.size()-1);
			messaggi.remove(messaggi.size()-1);
			return msg;
		}
	}
	
	
	private class ThreadListenerPerSocket extends Thread //questa classe si occupa di stare in ascolto sul server socket
	{
		@Override
		public void run() {
			
			for(;;)
			{
				try 
				{
					Socket connSock = servSocket.accept();
					//arrivata connessione
//					System.out.println("ARRIVATA CONNESSIONE SU PORTA "+indirizziNodi.get(id).intValue());
					ThreadLettoreCanale t1 = new ThreadLettoreCanale(connSock);
					t1.start();
				} 
				catch (IOException e) 
				{
					valid = false;
					e.printStackTrace();
				}
			}
		
		}
	}
	
	private class ThreadLettoreCanale extends Thread
	{
		Socket connSock;
		public ThreadLettoreCanale(Socket connSock)
		{
			this.connSock = connSock;
			
		}
		
		@Override
		public void run() {
			try 
			{
				connSock.setSoTimeout(0);
				BufferedReader read = new BufferedReader(new InputStreamReader(connSock.getInputStream()));
				String msg = read.readLine();
//				System.out.println("RICEVUTO "+msg);
				if(null != msg)
				{
					msgRicevuti.push(msg);
				}
			} 
			catch (IOException e) 
			{
				e.printStackTrace();
			}
			finally
			{
				try{connSock.close(); } catch(IOException ex) { }
			}
		}
	}
	
	private class ThreadScrittore extends Thread
	{
		int portaDestinatario;
		String msgToSend;
		public ThreadScrittore(int portaDestinatario,String msg)
		{
			this.msgToSend = msg;
			this.portaDestinatario = portaDestinatario;
		}
		
		@Override
		public void run() {
			Socket connSock = null;
			
			try 
			{
				connSock = new Socket(InetAddress.getByName(null),portaDestinatario);
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(connSock.getOutputStream()));
				
				if(!msgToSend.equals("marker"))
					System.out.println("NODO "+id+" INVIO "+msgToSend);
			
				writer.write(msgToSend);
				writer.flush(); //obbligatorio altrimenti non invia !
				messaggiStatoNodoInviati.add(msgToSend); 
				
				
			} 
			catch (UnknownHostException e) 
			{
				e.printStackTrace();
			} 
			catch (IOException e) 
			{
				e.printStackTrace();
			}
			finally
			{
				try{ if(connSock != null) connSock.close(); }
				catch(IOException ex){ ex.printStackTrace(); }
			}
		}
	}
	
}
