import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;


public class TestClass {
	public static void main(String[] args) throws InterruptedException
	{
		ArrayList<Integer> indirizziNodi = new ArrayList<Integer>();
		ArrayList<Thread> nodi = new ArrayList<Thread>();

		for(int i=0;i<10;i++) //10 nodi
		{
			//i sarà l'id del nodo
			nodi.add(new Nodo(i,indirizziNodi));
		}
		
		for(Thread el : nodi)
		{
			if(((Nodo)el).isValido() )
			{
				((Nodo)el).start();
			}
		}
		
		//attendo tot secondi, poi eleggo uno tra i nodi come l'iniziatore dello snapshot

		Thread.sleep(10000);
		int indRand =(int)( Math.random() * 10 );
		((Nodo)nodi.get(indRand) ).startSnapshot();

		 
		HashMap<Integer,ArrayList<String>> canaliPerMittente = new HashMap<Integer,ArrayList<String>>();
		
		for(int i=0;i<10;i++)
		{
			nodi.get(i).join();
		}
	
		//analisi recorded state
		for(int i=0;i<10;i++)
		{
			System.out.println("Messaggi (inviati) appartenenti al nodo con id "+i);
			ArrayList<String> msgInviati = ((Nodo)nodi.get(i)).messaggiStatoNodoInviati;
			for(String msg : msgInviati)
			{	if(msg.equals("marker"))
					continue;
				System.out.println(msg);
			}
			System.out.println("Messaggi (ricevuti) appartenenti al nodo con id "+i);
			ArrayList<String> msgRicevuti = ((Nodo)nodi.get(i)).messaggiStatoNodoRicevuti;
			for(String msg : msgRicevuti)
			{
				if(msg.equals("marker"))
					continue;
				System.out.println(msg);
			}
		
			//....stato canali (ordinati per mittente)
			
			for(Iterator<Integer> it = ((Nodo)nodi.get(i)).storicoMessaggiRicevutiPerCanali.keySet().iterator();  it.hasNext(); )
			{
				Integer idMittenteSuNodo = it.next();
				if( !canaliPerMittente.containsKey(idMittenteSuNodo) )
				{
					canaliPerMittente.put(idMittenteSuNodo,new ArrayList<String>());
				}
				for(String msg : ((Nodo)nodi.get(i)).storicoMessaggiRicevutiPerCanali.get(idMittenteSuNodo))
					canaliPerMittente.get(idMittenteSuNodo).add(msg);
			}
			
		
			System.out.println("------------------STATO CANALE IN ARRIVO SUL NODO CON ID "+i);		
			for(Iterator<Integer> it= ((Nodo)nodi.get(i)).storicoMessaggiRicevutiPerCanali.keySet().iterator(); it.hasNext(); )
			{
				Integer idMittente = it.next();
				System.out.println("IN ARRIVO DAL NODO "+idMittente+":");
				for(Iterator<String> it2 = ((Nodo)nodi.get(i)).storicoMessaggiRicevutiPerCanali.get(idMittente).iterator(); it2.hasNext(); )
				{
					System.out.println(it2.next());
				}
			}
			
		}
	
		
		
		
	}
	
	
	
	
}
