import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import es.uvigo.det.ro.simpledns.*;

public class dnsclient {

	public static final int PUERTO_SERVIDOR = 53; // Establecemos el puerto del servidor DNS
	static LinkedList<String> Cache = new LinkedList<String>();
	static String paracn="";

	public static void main(String[] args) {


		Scanner entrada = new Scanner(System.in);
		String consulta = null;
		String datos[]=null;
		while (entrada.hasNext()) {

			consulta = entrada.nextLine();
			datos=consulta.split(" ");


			if (args.length != 2) {
				System.err.println("LLAMADA ERRONEA: Debe ser del tipo \n dnsclient -(t/u) IP_Servidor");
				System.exit(-1);
			}
			String protocolo = args[0];
			String IP= args[1];

			Consulta(protocolo, IP, datos);


			System.out.println();
		}

	}

	private static void Consulta(String protocolo, String IP,String[] datos) {

		LinkedList<String> ListaDir = new LinkedList<String>();

		int d = 0;
		int salida = 0;
		int consultaAct = 0;
		int respC = 0;
		int contin = 0;
		boolean noesCNAME=true;
		boolean avisotcp=true;

		RRType tipo;
		String Nombre = datos[1];
		String GuardarNombre = datos[1];
		String GuardarIP = IP;

		switch (datos[0]) {
			case "A":
			tipo = RRType.A;
			break;

			case "AAAA":
			tipo = RRType.AAAA;
			break;

			case "NS":
			tipo = RRType.NS;
			break;

			case "CNAME":
			tipo=RRType.CNAME;
			break;
			
			default:
			tipo=RRType.CNAME;
			break;
		}

		while (salida == 0) {
			String time = null;
			time = currentTime();
			String respCache;


			InetAddress ipServ = null;
			try {
				ipServ = InetAddress.getByName(IP);
			} catch (Exception e) {
				// No hace nada
			}
			if (protocolo.equalsIgnoreCase("-u") || protocolo.equalsIgnoreCase("-t")) {


				//TCP no esta implementado se avisa al principio con esta condicion


				if (protocolo.equalsIgnoreCase("-t") && avisotcp){

					System.out.println("No se halla implementado el uso de TCP. Se usara UDP");
					avisotcp=false;
				}

				for (int e = 0; e < Cache.size(); e++) {
					if (Cache.get(e).contains("Q UDP " + IP + " " + tipo + " " + Nombre)
					&& (TTLvalido(Cache.get(e).split("->")[1], time,
					Cache.get(e).split("->")[2].split(" ")[3])))
					respC = 1;
				}
				if (respC == 1) {
					System.out.println("Q cache " + tipo + " " + Nombre);
					System.out.println();
				} else {
					System.out.println("Q UDP " + IP + " " + tipo + " " + Nombre);
				}

				for (int n = 0; n < Cache.size(); n++) {
					if (Cache.get(n).contains("Q UDP " + IP + " " + tipo + " " + Nombre) && (respC == 1)) {
						respCache = Cache.get(n).split("->")[2];
						System.out.println(respCache);
						salida = 1;
					}
				}
				if (salida == 1)
				continue;

				ListaDir.push("Q UDP " + IP + " " + tipo + " " + Nombre + "->" + time + "->");

				try {

					DatagramSocket datagrama = null;
					datagrama = new DatagramSocket();
					Message peticion = new Message(Nombre, tipo, false);

					byte[] pet_byte = peticion.toByteArray();
					datagrama.send(new DatagramPacket(pet_byte, pet_byte.length, ipServ, PUERTO_SERVIDOR));

					byte[] res_byte = new byte[1500];
					datagrama.receive(new DatagramPacket(res_byte, res_byte.length));

					Message respuesta = new Message(res_byte);

					if(respuesta.getAnswers().size() >0){

						if (respuesta.getAnswers().get(0).getRRType().toString().equals("CNAME")){

							for (ResourceRecord index : respuesta.getAnswers()) {
								volcarCache(index, IP, ListaDir);
								volcarRespuesta(System.out, index, IP);
							}

							tipo=RRType.A;
							IP=GuardarIP;
							Nombre=paracn;

							noesCNAME=false;
						}
						else{
							noesCNAME=true;

						}
					}
					else{
						noesCNAME=true;
					}
					if (respuesta.getAnswers().size() > 0 && noesCNAME) {

						if (tipo.toString().trim().compareTo("A") == 0) {
							for (ResourceRecord index : respuesta.getAnswers()) {
								volcarCache(index, IP, ListaDir);
								if (consultaAct == 1 && contin == 0) {
									if (respuesta.getAnswers().get(0).getRRType().toString().trim().equals("A")) {
										IP = volcarIPA((AResourceRecord) respuesta.getAnswers().get(0));
										Nombre = GuardarNombre;
										contin = 1;
										volcarRespuesta(System.out, index, IP);
										break;
									}
									else
									return;
								}
								System.out.println();
								volcarRespuesta(System.out, index, IP);
								return;
							}


							if (contin == 1){
								continue;

							}




						} else if (tipo.toString().trim().compareTo("NS") == 0) {
							System.out.println();
							for (ResourceRecord index : respuesta.getAnswers()) {
								volcarCache(index, IP, ListaDir);
								volcarRespuesta(System.out, index, IP);
							}



						} else if (tipo.toString().trim().compareTo("AAAA") == 0) {
							System.out.println();
							for (ResourceRecord index : respuesta.getAnswers()) {
								volcarCache(index, IP, ListaDir);
								volcarRespuesta(System.out, index, IP);
							}
						}

						else if (tipo.toString().trim().equals("CNAME")){
							System.out.println();

							for (ResourceRecord index : respuesta.getAnswers()) {

								volcarCache(index, IP, ListaDir);
								volcarRespuesta(System.out, index, IP);
							}

						}

						datagrama.close();
						salida = 1;
						continue;

					} else if (noesCNAME) {
						if (respuesta.getNameServers().size() <= 0) {
							System.out.println("No hay registro para la seccion NS");
							datagrama.close();
							salida = 1;
							continue;

						}
						if (respuesta.getNameServers().get(0).getRRType().toString().trim().compareTo("NS") != 0) {
							if (tipo.toString().trim().equals("AAAA")) {
								System.out.println("No hay servidores del tipo AAAA");
							} else {
								System.out.println("No hay servidores del tipo NS");
							}
							datagrama.close();
							salida = 1;
							continue;

						}
						int TTL = respuesta.getNameServers().get(0).getTTL();
						String Name = NS((NSResourceRecord) respuesta.getNameServers().get(d));
						String Type = respuesta.getNameServers().get(0).getRRType().toString();

						System.out.println("A " + IP + " " + Type + " " + TTL + " " + Name);

						if (respuesta.getAdditonalRecords().size() <= 0) {

							if (tipo.toString().trim().equals("A")) {

								LinkedList<String> restoNombres = new LinkedList<String>();

								IP = GuardarIP;

								Nombre = getDirNS(respuesta.getNameServers());
								System.out.println("No hay registro tipo A en seccion ADDITIONAL para "+Nombre);
								for (int i=1; i<respuesta.getNameServers().size();i++){
									String Nombresextra=getDirNSExtra(respuesta.getNameServers().get(i));
									restoNombres.add(Nombresextra);

								}

								for (int i=0; i<restoNombres.size(); i++){
									System.out.println("No hay registro tipo A en seccion ADDITIONAL para "+restoNombres.get(i));
								}
								consultaAct = 1;
								continue;

							} else if (tipo.toString().trim().equals("NS")) {

								System.out.println();
								for (ResourceRecord index : respuesta.getNameServers()) {
									volcarRespuesta(System.out, index, IP);
									volcarCache(index, IP, ListaDir);
								}
								datagrama.close();
								salida = 1;
								continue;


							} else if (tipo.toString().trim().equals("AAAA")) {
								System.out.println("No hay registro para la seccion ADDITIONAL");
								datagrama.close();
								salida = 1;
								continue;
							}

							else if (tipo.toString().trim().equals("CNAME")){

								System.out.println();
								for (ResourceRecord index : respuesta.getNameServers()) {
									volcarCache(index, IP, ListaDir);
									volcarRespuesta(System.out, index, IP);
									datagrama.close();
									continue;
								}
							}

						}
						for (int i = 0; i < respuesta.getAdditonalRecords().size(); i++) {
							if (respuesta.getAdditonalRecords().get(i).getRRType().toString().trim()
							.compareTo("A") == 0) {
								if (A((AResourceRecord) respuesta.getAdditonalRecords().get(i)).trim()
								.equalsIgnoreCase(Name)) {
									String dir = getDireccion((AResourceRecord) respuesta.getAdditonalRecords().get(i));
									String type2 = respuesta.getAdditonalRecords().get(i).getRRType().toString();
									System.out.println("A " + IP + " " + type2 + " " + TTL + " " + dir);
									IP = dir;
									break;

								}
							}

						}

					}
					datagrama.close();

				} catch (Exception e) {
					System.err.println("Error: " + e.getMessage());

				}

			} else {
				salida=1;
				continue;
			}
		}
	}

	//Para el servidor NS a elegir si no tiene additional

	private static String getDirNS(List<ResourceRecord> NS) {
		String stringNS = null;
		for (int i = 0; i < NS.size(); i++) {
			if (NS.get(i).getRRType().toString().trim().equals("NS")) {
				stringNS = NS((NSResourceRecord) (NS.get(i)));
				break;
			}
		}
		return stringNS;
	}

	// Para el resto de servidores NS si no tienen additional

	private static String getDirNSExtra(ResourceRecord NS) {
		String stringNS = "";

		if (NS.getRRType().toString().trim().equals("NS")) {
			stringNS = NS((NSResourceRecord) NS );
		}

		return stringNS;
	}

	private static boolean TTLvalido(String time1, String time2, String TTL) {
		int segundos1 = ((Integer.parseInt(time1.split(":")[0]) * 3600) + (Integer.parseInt(time1.split(":")[1]) * 60)
		+ (Integer.parseInt(time1.split(":")[2])));
		int segundos2 = ((Integer.parseInt(time2.split(":")[0]) * 3600) + (Integer.parseInt(time2.split(":")[1]) * 60)
		+ (Integer.parseInt(time2.split(":")[2])));
		int segundosTTL = Integer.parseInt(TTL);
		int resta = (segundos2 - segundos1);

		if ((resta) > (segundosTTL)) {
			return false;
		} else
		return true;
	}

	private static String currentTime() {
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		return sdf.format(cal.getTime());
	}

	private static void volcarCache(ResourceRecord rr, String IP, LinkedList<String> lista) {
		String Ans = ("A " + IP + " " + rr.getRRType() + " " + rr.getTTL() + " ");
		String wer = null;
		switch (rr.getRRType()) {

			case A:
			wer = devolverA((AResourceRecord) rr);
			break;

			case AAAA:
			wer = devolverAAAA((AAAAResourceRecord) rr);
			break;

			case NS:
			wer = devolverNS((NSResourceRecord) rr);
			break;

			case CNAME:
			wer= devolverCNAME((CNAMEResourceRecord) rr);
			break;

			default:
			System.out.println("Tipo no valido.");
			break;
		}

		for (int i = 0; i < lista.size(); i++) {
			Cache.push(lista.get(i) + Ans + wer);
		}
	}

	private static void volcarRespuesta(PrintStream bus, ResourceRecord rr, String IP) {
		bus.print("A " + IP + " " + rr.getRRType() + " " + rr.getTTL() + " ");

		switch (rr.getRRType()) {

			case A:
			volcarA(bus, (AResourceRecord) rr);
			break;

			case AAAA:
			volcarAAAA(bus, (AAAAResourceRecord) rr);
			break;

			case NS:
			volcarNS(bus, (NSResourceRecord) rr);
			break;

			case CNAME:
			volcarCNAME(bus,(CNAMEResourceRecord) rr);
			paracn=devolverCNAME((CNAMEResourceRecord) rr);
			break;

			default:
			bus.println("Tipo no valido.");
		}
	}

	private static String getDireccion(AResourceRecord rr) {
		return rr.getAddress().getHostAddress().toString();
	}

	private static String A(AResourceRecord rr) {
		return rr.getDomain().toString();
	}

	private static String devolverA(AResourceRecord rr) {
		return rr.getAddress().getHostAddress();
	}

	private static void volcarA(PrintStream bus, AResourceRecord rr) {
		bus.println(rr.getAddress().getHostAddress());
	}

	private static String volcarIPA(AResourceRecord rr) {
		return (rr.getAddress().getHostAddress());
	}

	private static String devolverAAAA(AAAAResourceRecord rr) {
		return rr.getAddress().getHostAddress();
	}

	private static void volcarAAAA(PrintStream bus, AAAAResourceRecord rr) {
		bus.println(rr.getAddress().getHostAddress());
	}

	private static String NS(NSResourceRecord rr) {
		return rr.getNS().toString();
	}

	private static String devolverNS(NSResourceRecord rr) {
		return rr.getNS().toString();
	}

	private static void volcarNS(PrintStream bus, NSResourceRecord rr) {
		bus.println(rr.getNS());

	}

	private static String CNAME(CNAMEResourceRecord rr){
		return rr.getCNAME().toString();
	}

	private static String devolverCNAME (CNAMEResourceRecord rr){
		return rr.getCNAME().toString();
	}

	private static void volcarCNAME(PrintStream bus, CNAMEResourceRecord rr) {
		bus.println(rr.getCNAME());

	}

}
