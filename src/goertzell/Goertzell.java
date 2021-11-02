/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package goertzell;

import java.io.*;
import static java.lang.Math.PI;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import java.net.*;
import java.util.Date;

/**
 *
 * @author Renato
 */
public class Goertzell {
    //Porta do EEG e do programa
    static int PORTA_SERVER = 11111;
    final String IP_CAP = "172.20.98.88";
    static final int MAX_TAM_PACOTE = 1500;
    //IP do aparelho de EEG
    static byte[] bIpEEG = {(byte) 192, (byte) 168, 1, 4};
    //aqui abaixo coloca o ip do wireless ou da rede local
    static byte[] bIpCAP = {(byte) 192, (byte) 168, 1, 6};
    static int nivelDebug = 1;

    public static void main(String[] args) throws Exception {
        int portaCAP = 0;
        int numero_pacotes = 30;
        DatagramSocket serverSocket = new DatagramSocket(PORTA_SERVER);
        byte[] dados = new byte[MAX_TAM_PACOTE];
        InetAddress ipEEG = InetAddress.getByAddress(bIpEEG);
        InetAddress ipCAP = InetAddress.getByAddress(bIpCAP);

        FileWriter writer = new FileWriter("Arquivo1.txt");
        PrintWriter saida = new PrintWriter(writer);
        int cont = 0;
        int ch = 35;

        int t = 0;

        System.out.print("IP do EEG: ");
        System.out.println(ipEEG.getHostAddress());

        System.out.print("IP do Captacoes: ");
        System.out.println(ipCAP.getHostAddress());
        System.out.println("teste");

        if ((args.length == 2) && args[0].equals("-p")) {
            numero_pacotes = Integer.parseInt(args[1]);
        }

        short[][][] m = new short[numero_pacotes][36][20];
        float[][][] mProcessaCombo = new float[4][2400][36];

        while (true) {
            System.out.println("teste");
            DatagramPacket pacoteRecebido = new DatagramPacket(dados, dados.length);

            serverSocket.receive(pacoteRecebido);

            InetAddress ipRecebido = pacoteRecebido.getAddress();

            //Se a porta do captacoes ainda nao foi atribuida
            //e o pacote veio do catacoes
            //ATRIBUI O VALOR DA PORTA DO CAPTACOES
            if ((portaCAP == 0) && (ipRecebido.getHostAddress().equals(ipCAP.getHostAddress()))) {
                portaCAP = pacoteRecebido.getPort();
            }

            //imprimeDadosRecebimento(pacoteRecebido)
            //imprimePacote(pacoteRecebido.getData(), pacoteRecebido.getLength());
            if (ipRecebido.getHostAddress().equals(ipCAP.getHostAddress())) {


                //Se recebeu do aparelho
                //envia para o servidor
                envia(serverSocket, ipEEG, PORTA_SERVER, pacoteRecebido.getData(), pacoteRecebido.getLength());

                if ((nivelDebug & 2) > 0) {
                    imprimeDadosRecebimento(pacoteRecebido);
                }

                if ((nivelDebug & 8) > 0) {
                    imprimePacote(pacoteRecebido.getData(), pacoteRecebido.getLength());
                }
            } else if (ipRecebido.getHostAddress().equals(ipEEG.getHostAddress())) {

                //Se recebeu do aparelho
                //envia para o servidor
                envia(serverSocket, ipCAP, portaCAP, pacoteRecebido.getData(), pacoteRecebido.getLength());

                int qtdAmostras = 0;

                if ((cont > 3) && (cont < (numero_pacotes + 4))) {
                    int bi = 103;	// Update pointers on new packet
                    int bf = bi - 71;
                    // Manipulate 1472 bytes of one buffer
                    System.out.println("Montando Matriz!");
                    for (int j = 0; j < 20; j++) {
                        //System.out.println("oi");
                        for (int i = bi; i > bf; i -= 2) {
                            //System.out.println(ch);
                            //System.out.println(j);
                            m[cont - 4][ch][j] = ccat(dados[i], dados[i - 1]);
                            ch--;
                        }
                        bi += 72;
                        bf = bi - 71;
                        //j++;
                        ch = 35;
                    } // Output: m[36channels][20instantsOfTime]

                } else if (cont == (numero_pacotes + 4)) {
                    writer = new FileWriter("Arquivo1.txt");
                    saida = new PrintWriter(writer);
                    //System.out.println("Inicio da gravacao:");
                    //long t0 =d.getTime();
                    long t0 = new Date().getTime();

                    float[][] matDada = new float[600][36];
                    //variável para poder alimentar as 600 amostras da matriz a ser processada
                    int amostras = 0;

                    for (int k = 0; k < numero_pacotes; k++) {
                        for (int i = 0; i < 20; i++) {
                            for (int j = 0; j < 36; j++) {
                                matDada[amostras][j] = m[k][i][j];
                                saida.print(m[k][j][i]);
                                saida.print(",");
                            };
                            amostras++;
                            saida.println();
                        }
                    }
                    saida.close();
                    writer.close();
                    System.out.println("Arquivo Escrito e Fechado!");
                    cont = 3;
                    System.out.println(new Date().getTime() - t0);

                    if (qtdAmostras >= 4) {

                        mProcessaCombo[0] = mProcessaCombo[1];
                        mProcessaCombo[1] = mProcessaCombo[2];
                        mProcessaCombo[2] = mProcessaCombo[3];
                        mProcessaCombo[3] = matDada;

                        /*
                        pego a matriz de apoio que tem os 4 ultimos segundos de processamento e
                        transformo ela em uma matriz 2400x36 para enviar para o processamento
                         */
                        float[][] mProcessa = new float[2400][36];
                        int segundo = 0;
                        int amtrs = 0;
                        for (int l = 0; l < 2400; l++) {
                            for (int c = 0; c < 36; c++) {
                                mProcessa[l][c] = mProcessaCombo[segundo][amtrs][c];
                            }
                            if ((l == 599) || (l == 1199) || (l == 1799) || (l == 2399)) {
                                segundo++;
                                amtrs = 0;
                            }
                        }
//                        aqui eu chamo a função que começa a processar os dados;
                        ProcessaGoertizel(mProcessa);
                    } else {
                        mProcessaCombo[qtdAmostras] = matDada;
                    }
                    qtdAmostras++;
                }
                cont++;
                //System.out.print("Recebido pacote numero: ");
                //System.out.println(cont);

                if ((nivelDebug & 4) > 0) {
                    imprimeDadosRecebimento(pacoteRecebido);
                }

                if ((nivelDebug & 16) > 0) {
                    imprimePacote(pacoteRecebido.getData(), pacoteRecebido.getLength());
                }
            }

        }//while(true)

    }//Fim do main

    static int ProcessaGoertizel(float[][] matDada) {
//        float[][] matDada = new float[2400][36];

        //sepera as freq. de estimulo
        float[] freq = new float[4];
        freq[0] = (float) 5.6;
        freq[1] = (float) 6.4;
        freq[2] = (float) 6.9;
        freq[3] = (float) 8.0;

        //cria uma matriz de uma dimensão apra enviar para goertzel (com as 2400 amostras referente aos 4 segundos.
        float[] matProcessa = new float[2400];

        float data;
        float[] maiorFreq = new float[36];
        float[] freqEscolhida = new float[36];

//        percorre todos os 36 canais do sinal, buscando saber qual é o melhor.
        for (int j = 0; j < 36; j++) {

            //for que percorre as 2400 linhas da matriz pegando ela para colocar em uma unica matriz para enviar para goertzel;
            for (int x = 0; x < 2400; x++) {
                matProcessa[x] = (float) matDada[x][j];
            }

            for (int y = 0; y < 4; y++) {
                data = goertzel(matProcessa, freq[y], 100);

                if (maiorFreq[j] == 0) {
                    maiorFreq[j] = data;
                    freqEscolhida[j] = freq[y];
                } else {
                    if (data > maiorFreq[j]) {
                        maiorFreq[j] = data;
                        freqEscolhida[j] = freq[y];
                    }
                }
            }
        }

        //aqui ele percorre a matriz que tem as maiores frequências dos 36 canais
        // Vamos usar apenas os canais oz, o1 e O2??
//        aqui eu pego os 3 melhores canais;
//        eu velho qual é o máximo de cada canal e pego as frequências relativas aos 3 máximos
        float[] melhor_canal = new float[3];
        // roda 3 vezes para pegar os 3 máximos dos 36 canais
        //variavel que defi
        float freqParaComando = 0;
        for (int mc = 0; mc < 3; mc++) {
            for (int t = 0; t < 36; t++) {
                if (maiorFreq[t] > melhor_canal[mc]) {
                    melhor_canal[mc] = freqEscolhida[t];
                }
            }
        }

        //verifico se os 3 canais melhores são iguais
        if (melhor_canal[0] == melhor_canal[1] && melhor_canal[0] == melhor_canal[2] && melhor_canal[1] == melhor_canal[2]) {
            for (int fr = 0; fr < 4; fr++) {
                if (freqParaComando == freq[fr]) {
                    enviaComando(fr + 1);
                }
            }
        }

        //temque retornar o valor da frequência processada;
        return 0;
    }

    static float goertzel(float[] x, float k, float N) {

        float tamanho;
        float lixo;
        float cost;
        float coefk;
        float sent;
        float tamanh;

        tamanh = x.length;

        cost = (float) cos(2 * PI * k / N);
        coefk = cost * 2;
        sent = (float) sin(2 * PI * k / N);

        float z_1 = 0;
        float z_2 = 0;
//        float[] ymag = zeros(tamanh / N, 1);
        float[] ymag = {0};
        float[] yreal = ymag;
        float[] yimg = ymag;

        float[] kk = ymag;

        int cont_1 = 1;
        int cont_2 = 1;
        int cont_3 = 1;

        while (cont_1 < tamanh) {
            for (int i = 1; i <= N; i++) {

                if (cont_1 < (tamanh + 1)) {
                    float z0 = coefk * z_1 - z_2 + x[i];
                    z_2 = z_1;
                    z_1 = z0;
                }
                cont_1++;
            }

            yreal[cont_3] = ((z_1 - z_2 * cost) / N);
            yimg[cont_3] = (z_2 * sent) / N;

            float yr = (yreal[cont_3] * yreal[cont_3]);
            float yi = ((yimg[cont_3]) * (yimg[cont_3]));
            ymag[cont_3] = (float) sqrt(yr + yi);

            z_1 = 0;
            z_2 = 0;

            cont_3++;
        }

        float maior = 0;
        for (int i = 0; i <= ymag.length; i++) {
            if (ymag[i] > maior) {
                maior = ymag[i];
            }
        }
        return maior;
    }

    static void enviaComando(int comando) {
        switch (comando) {
            case 1:
                // usado para freq 1 -> Movimento p/cima
                break;
            case 2:
                // usado para freq 2 -> Movimento p/direita
            case 3:
                // usado para freq 3 -> Movimento p/baixo
            case 4:
                // usado para freq 4 -> Movimento p/esquerda
                break;
            default:
        }

    }

    static short ccat(byte b1, byte b2) {
        short result;
        //result = ((short) b1);
        result = (short) ((b1 << 8) + b2);
        return result;
    }

    static void envia(DatagramSocket socket, InetAddress endDestino, int portaDestino, byte[] dados, int tamDados) throws IOException {
        DatagramPacket pacoteEnviado = new DatagramPacket(dados, tamDados, endDestino, portaDestino);
        try {
            socket.send(pacoteEnviado);
        } catch (Exception e) {
            System.out.printf("Erro: .\n", e);
        }

    }

    static void imprimePacote(byte[] dados, int tam) {
        int i;
        System.out.println("------------------------------------");
        System.out.println("Pacote:");
        for (i = 0; i < tam; i++) {
            System.out.printf("%x ", dados[i]);
        }
        System.out.println("");
        System.out.println("------------------------------------");
    }

    static void imprimeDadosRecebimento(DatagramPacket pacoteRecebido) {
        System.out.println("************************************");
        System.out.print("Pacote recebido de: ");
        imprimeDadosPacote(pacoteRecebido);
        System.out.println("************************************");
    }

    static void imprimeDadosEnvio(DatagramPacket pacoteEnviado) {
        System.out.println("************************************");
        System.out.print("Pacote enviado para: ");
        imprimeDadosPacote(pacoteEnviado);
        System.out.println("************************************");
    }

    static void imprimeDadosPacote(DatagramPacket pacote) {
        System.out.print(pacote.getAddress().getHostAddress());
        System.out.print(":");
        System.out.print(pacote.getPort());
        System.out.print(" - Tamanho do pacote: ");
        System.out.println(pacote.getLength());
    }
}
