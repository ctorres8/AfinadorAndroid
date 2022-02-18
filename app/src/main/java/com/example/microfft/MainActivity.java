package com.example.microfft;

import static java.lang.Math.log;
import static java.lang.Math.sqrt;

import android.Manifest;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.jtransforms.fft.DoubleFFT_1D;

public class MainActivity extends AppCompatActivity {

    //---------------------------------------------------------------------------------------
    //-------------------------- Declaraciones Generales ------------------------------------
    //---------------------------------------------------------------------------------------

    // Flags de control de la aplicacion
    private boolean stopped = true;

    //Botones
    private boolean flag_miG = false; //-------------MI
    private boolean flag_si = false; //-------------Si
    private boolean flag_sol = false; //-------------Sol
    private boolean flag_re = false; //-------------Re
    private boolean flag_la = false; //-------------La
    private boolean flag_miA = false; //-------------mi
    private boolean flag_cal = false; // CALIBRACION

    private boolean modo_Cal_inf =false;
    private boolean modo_Cal_sup =false;
    //NOTAS
    /*
    Los valores fueron sacados de internet midiendo con un analizador para celu
    sacado de google play. Nombre "FFT Spectrum".
    link de la pagina de notas afinadas: https://tuner-online.com/es/
     */
    private float DELTA = 5.0f;     //Hz
    private float NOTA_MIA = 329.63f;  //Hz
    private float NOTA_SI = 246.94f;  //Hz
    private float NOTA_SOL = 196.0f;   //Hz
    private float NOTA_RE = 146.83f;  //Hz
    private float NOTA_LA = 110.0f;   //Hz
    private float NOTA_MIG = 82.41f;   //Hz

    private float NOTA_CAL_INF0 = 697.0f; //Hz
    private float NOTA_CAL_INF1 = 770.0f; //Hz
    private float NOTA_CAL_INF2 = 852.0f; //Hz
    private float NOTA_CAL_INF3 = 941.0f; //Hz
    private float NOTA_CAL_SUP0 = 1209.0f;//Hz
    private float NOTA_CAL_SUP1 = 1336.0f;//Hz
    private float NOTA_CAL_SUP2 = 1477.0f;//Hz

    // Con este flag avisamos que hay data nueva a la FFT, es un semaforo mal hecho
    boolean buffer_ready = false;

    //Variables para encontrar los picos y la fundamental
    private float f_fundamental = 0.0f; //Hz
    private int NIVELMINIMO = 275;//250;
    private float PASOS = 0.336499f; // pasos para 2048
    private float FRECUENCIA_MIN = 80;//Hz

    private float FREC_ELEGIDA = 80;//Hz

    private float FREC_INICIO= 0.0f; //Hz

    // Defino los buffers, potencia de 2 para mas placer y por la FFT
    private int POW_FREC_SHOW = 11;
    private int POW_FFT_BUFFER = 16;


    //Tamaños de Buffer
    private int BUFFER_SIZE_SHOW_FREQ = (int) Math.pow(2, POW_FREC_SHOW);
    private int BUFFER_SIZE = (int) Math.pow(2, POW_FFT_BUFFER);
    private float[] bufferFreq = new float[BUFFER_SIZE_SHOW_FREQ];

    //Hanning
    private float[] hanning = new float[BUFFER_SIZE];

    //Flag de estado de botones
    private float FREC_PREV = FRECUENCIA_MIN;


    //---------------------------------------------------------------------------------------
    //-------------------------- Libreria FFT JAVA ------------------------------------------
    //---------------------------------------------------------------------------------------
    // Creamos la clase para hacer la FFT
    // ver:  https://github.com/wendykierp/JTransforms
    // Para que esto ande debemos poner la ependencia en "build.gradle (Module: app)" :
    // dentro de "dependencies" ponemos:
    // implementation 'com.github.wendykierp:JTransforms:3.1'
    private DoubleFFT_1D fft = new DoubleFFT_1D(BUFFER_SIZE);
    // Este es el buffer de entrada a la FFT, que quiere doubles...
    double[] buffer_double = new double[BUFFER_SIZE];


    //---------------------------------------------------------------------------------------
    //-------------------------- Captura de audio -------------------------------------------
    //---------------------------------------------------------------------------------------
    // Declaramos la clase para grabar audio
    AudioRecord recorder = null;
    private int SAMPLE_RATE = 44100; // en Hz
    // Buffer donde sale el valor crudo del microfono
    short[] buffer = new short[BUFFER_SIZE];

    //---------------------------------------------------------------------------------------
    //-------------------------- Permisos de audio ------------------------------------------
    //---------------------------------------------------------------------------------------
    // Estas funciones de aca abajo salen de la documentación de Android, es un metodo
    // que pide permisos de microfono

    // Flag del pedido
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;

    // Pedimos permiso para grabar audio RECORD_AUDIO
    private boolean permissionToRecordAccepted = false;
    private String[] permissions = {Manifest.permission.RECORD_AUDIO};

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_RECORD_AUDIO_PERMISSION:
                permissionToRecordAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        if (!permissionToRecordAccepted) finish();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bloqueo la pantalla en modo retrado
        setRequestedOrientation((ActivityInfo.SCREEN_ORIENTATION_PORTRAIT));


        final Button btn_mig = findViewById(R.id.boton_MIG);
        final Button btn_la = findViewById(R.id.boton_LA);
        final Button btn_re = findViewById(R.id.boton_RE);
        final Button btn_sol = findViewById(R.id.boton_SOL);
        final Button btn_si = findViewById(R.id.boton_SI);
        final Button btn_mia = findViewById(R.id.boton_MIA);
        

        //Accion al tocar el boton MI GRAVE
        btn_mig.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                // Cuerda Mi Grave
                FRECUENCIA_MIN = NOTA_MIG - 2 * DELTA;
                FREC_ELEGIDA = NOTA_MIG;

                //Esto es para limpiar el color de los botones (Todos vuelven a gris)
                btn_mig.setText("E");
                btn_mig.setBackgroundColor(getResources().getColor(R.color.buttom_off));
                btn_la.setText("A");
                btn_la.setBackgroundColor(getResources().getColor(R.color.buttom_off));
                btn_re.setText("D");
                btn_re.setBackgroundColor(getResources().getColor(R.color.buttom_off));
                btn_sol.setText("G");
                btn_sol.setBackgroundColor(getResources().getColor(R.color.buttom_off));
                btn_si.setText("B");
                btn_si.setBackgroundColor(getResources().getColor(R.color.buttom_off));
                btn_mia.setText("e");
                btn_mia.setBackgroundColor(getResources().getColor(R.color.buttom_off));

                if (!flag_miG) {
                    flag_miG = true;
                    btn_mig.setBackgroundColor(getResources().getColor(R.color.buttom_on));
                    btn_mig.setText("E");
                }
                else {
                    flag_miG = false;
                    btn_mig.setText("E");
                    btn_mig.setBackgroundColor(getResources().getColor(R.color.buttom_off));
                }

                if(flag_la||flag_re||flag_sol||flag_si||flag_miA){
                    //Si hay +1 boton elegido, me quedo con el +nuevo pero no dejo de escuchar
                    flag_la = false;flag_re = false;flag_sol = false;flag_si = false;flag_miA = false;
                }
                else accion_boton_ini_fin(); //Enciendo/Apago el microfono
            }
        });

        //Accion al tocar el boton LA
        btn_la.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // Cuerda LA
                FRECUENCIA_MIN = NOTA_LA - 2 * DELTA;
                FREC_ELEGIDA = NOTA_LA;

                //Esto es para limpiar el color de los botones (Todos vuelven a gris)
                btn_mig.setText("E");
                btn_mig.setBackgroundColor(getResources().getColor(R.color.buttom_off));
                btn_la.setText("A");
                btn_la.setBackgroundColor(getResources().getColor(R.color.buttom_off));
                btn_re.setText("D");
                btn_re.setBackgroundColor(getResources().getColor(R.color.buttom_off));
                btn_sol.setText("G");
                btn_sol.setBackgroundColor(getResources().getColor(R.color.buttom_off));
                btn_si.setText("B");
                btn_si.setBackgroundColor(getResources().getColor(R.color.buttom_off));
                btn_mia.setText("e");
                btn_mia.setBackgroundColor(getResources().getColor(R.color.buttom_off));

                if (!flag_la) {
                    //stopped=true;
                    flag_la = true;
                    btn_la.setBackgroundColor(getResources().getColor(R.color.buttom_on));
                    btn_la.setText("A");
                }
                else {
                    flag_la = false;
                    btn_la.setText("A");
                    btn_la.setBackgroundColor(getResources().getColor(R.color.buttom_off));
                }

                if(flag_miG||flag_re||flag_sol||flag_si||flag_miA){
                    //Si hay +1 boton elegido, me quedo con el +nuevo pero no dejo de escuchar
                    flag_miG = false;flag_re = false;flag_sol = false;flag_si = false;flag_miA = false;
                }
                else accion_boton_ini_fin(); //Enciendo/Apago el microfono

            }
        });

        //Accion al tocar el boton RE
        btn_re.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // Cuerda RE
                FRECUENCIA_MIN = NOTA_RE - 2 * DELTA;
                FREC_ELEGIDA = NOTA_RE;

                //Esto es para limpiar el color de los botones (Todos vuelven a gris)
                btn_mig.setText("E");
                btn_mig.setBackgroundColor(getResources().getColor(R.color.buttom_off));
                btn_la.setText("A");
                btn_la.setBackgroundColor(getResources().getColor(R.color.buttom_off));
                btn_re.setText("D");
                btn_re.setBackgroundColor(getResources().getColor(R.color.buttom_off));
                btn_sol.setText("G");
                btn_sol.setBackgroundColor(getResources().getColor(R.color.buttom_off));
                btn_si.setText("B");
                btn_si.setBackgroundColor(getResources().getColor(R.color.buttom_off));
                btn_mia.setText("e");
                btn_mia.setBackgroundColor(getResources().getColor(R.color.buttom_off));

                if (!flag_re) {
                   // stopped=true;
                    flag_re = true;
                    btn_re.setBackgroundColor(getResources().getColor(R.color.buttom_on));
                    btn_re.setText("D");
                }
                else {

                    flag_re = false;

                    btn_re.setText("D");
                    btn_re.setBackgroundColor(getResources().getColor(R.color.buttom_off));
                }

                if(flag_miG||flag_la||flag_sol||flag_si||flag_miA){
                    //Si hay algun flag colgado que quedo levantado, lo bajo
                    flag_miG = false;flag_la = false;flag_sol = false;flag_si = false;flag_miA = false;
                }
                else accion_boton_ini_fin(); //Enciendo/Apago el microfono
            }
        });

        //Accion al tocar el boton SOL
        btn_sol.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // Cuerda SOL
                FRECUENCIA_MIN = NOTA_SOL - 2 * DELTA;
                FREC_ELEGIDA = NOTA_SOL;

                //Esto es para limpiar el color de los botones (Todos vuelven a gris)
                btn_mig.setText("E");
                btn_mig.setBackgroundColor(getResources().getColor(R.color.buttom_off));
                btn_la.setText("A");
                btn_la.setBackgroundColor(getResources().getColor(R.color.buttom_off));
                btn_re.setText("D");
                btn_re.setBackgroundColor(getResources().getColor(R.color.buttom_off));
                btn_sol.setText("G");
                btn_sol.setBackgroundColor(getResources().getColor(R.color.buttom_off));
                btn_si.setText("B");
                btn_si.setBackgroundColor(getResources().getColor(R.color.buttom_off));
                btn_mia.setText("e");
                btn_mia.setBackgroundColor(getResources().getColor(R.color.buttom_off));

                if (!flag_sol) {
                    flag_sol = true;
                    btn_sol.setBackgroundColor(getResources().getColor(R.color.buttom_on));
                    btn_sol.setText("G");
                }
                else {
                    flag_sol = false;
                    btn_sol.setText("G");
                    btn_sol.setBackgroundColor(getResources().getColor(R.color.buttom_off));
                }

                if(flag_miG||flag_la||flag_re||flag_si||flag_miA){
                    //Si hay +1 boton elegido, me quedo con el +nuevo pero no dejo de escuchar
                    flag_miG = false;flag_la = false;flag_re = false;flag_si = false;flag_miA = false;
                }
                else accion_boton_ini_fin(); //Enciendo/Apago el microfono
            }
        });

        //Accion al tocar el boton SI
        btn_si.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // Cuerda SI
                FRECUENCIA_MIN = NOTA_SI - 2 * DELTA;
                FREC_ELEGIDA = NOTA_SI;

                //Esto es para limpiar el color de los botones (Todos vuelven a gris)
                btn_mig.setText("E");
                btn_mig.setBackgroundColor(getResources().getColor(R.color.buttom_off));
                btn_la.setText("A");
                btn_la.setBackgroundColor(getResources().getColor(R.color.buttom_off));
                btn_re.setText("D");
                btn_re.setBackgroundColor(getResources().getColor(R.color.buttom_off));
                btn_sol.setText("G");
                btn_sol.setBackgroundColor(getResources().getColor(R.color.buttom_off));
                btn_si.setText("B");
                btn_si.setBackgroundColor(getResources().getColor(R.color.buttom_off));
                btn_mia.setText("e");
                btn_mia.setBackgroundColor(getResources().getColor(R.color.buttom_off));

                if (!flag_si) {
                    flag_si = true;
                    btn_si.setBackgroundColor(getResources().getColor(R.color.buttom_on));
                    btn_si.setText("B");
                }
                else {
                    flag_si = false;
                    btn_si.setText("B");
                    btn_si.setBackgroundColor(getResources().getColor(R.color.buttom_off));
                }

                if(flag_miG||flag_la||flag_re||flag_sol||flag_miA){
                    //Si hay +1 boton elegido, me quedo con el +nuevo pero no dejo de escuchar
                    flag_miG = false;flag_la = false;flag_re = false;flag_sol = false;flag_miA = false;
                }
                else accion_boton_ini_fin(); //Enciendo/Apago el microfono
            }
        });

        //Accion al tocar el boton MI AGUDO
        btn_mia.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Cuerda MI Agudo
                FRECUENCIA_MIN = NOTA_MIA - 2 * DELTA;
                FREC_ELEGIDA = NOTA_MIA;

                //Esto es para limpiar el color de los botones (Todos vuelven a gris)
                btn_mig.setText("E");
                btn_mig.setBackgroundColor(getResources().getColor(R.color.buttom_off));
                btn_la.setText("A");
                btn_la.setBackgroundColor(getResources().getColor(R.color.buttom_off));
                btn_re.setText("D");
                btn_re.setBackgroundColor(getResources().getColor(R.color.buttom_off));
                btn_sol.setText("G");
                btn_sol.setBackgroundColor(getResources().getColor(R.color.buttom_off));
                btn_si.setText("B");
                btn_si.setBackgroundColor(getResources().getColor(R.color.buttom_off));
                btn_mia.setText("e");
                btn_mia.setBackgroundColor(getResources().getColor(R.color.buttom_off));

                if (!flag_miA) {
                    flag_miA =true;
                    btn_mia.setBackgroundColor(getResources().getColor(R.color.buttom_on));
                    btn_mia.setText("e");
                }
                else {
                    flag_miA = false;
                    btn_mia.setText("e");
                    btn_mia.setBackgroundColor(getResources().getColor(R.color.buttom_off));
                }

                if(flag_la||flag_re||flag_sol||flag_si||flag_miG){
                    //Si hay +1 boton elegido, me quedo con el +nuevo pero no dejo de escuchar
                    flag_miG = false;flag_la = false;flag_re = false;flag_sol = false;flag_si = false;
                }
                else accion_boton_ini_fin(); //Enciendo/Apago el microfono
            }
        });




        // Pedimos permiso para grabar audio
        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);

        //Creo el grabador
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }


        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_DEFAULT,
                AudioFormat.ENCODING_PCM_16BIT,
                BUFFER_SIZE);

        //Hanning - Ventana
        for (int i=0; i<BUFFER_SIZE;i++) {
            //Funcion sacada de MATLAB
            hanning[i]=(float)(0.5-(0.5*Math.cos((2*Math.PI*i)/(BUFFER_SIZE-1) )));
        }

        //Relleno del buffer mapeando las frecuencias
        bufferFreq[0]=FREC_INICIO;
        for(int i=1;i<BUFFER_SIZE_SHOW_FREQ;i++) {
            // El valor 2,353 se saco de manera expiremental midiendo distintas
            // frecuencias y ajustando hasta tener un valor aceptable.
            bufferFreq[i] = bufferFreq[i-1] + PASOS;
        }

        //------------------------------------------------------------------------------------------
        //-------------------------- EJECUTO THREADS DE PROCESO ------------------------------------
        //------------------------------------------------------------------------------------------
        // Como tiene que funcionar en paralelo, necesitamos un par de threads

        //---------------------------------- CALCULO FFT ------------------------------------------
        // Este thread espera que el grabador de audio termine y hace la FFT. Solo mira el flag,
        // si esta en flase vuelve a dormir y si es true hace FFT.
        // La FFT usada va a depender del checkbox, que setea el flag
        new Thread(new Runnable() {

            @Override
            public void run()
            {
                while (true)
                {
                    if (!stopped) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                calcFFT();
                            }
                        });
                    }

                    // sleep
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {

                    }
                }
            }
        }).start();
            //------------------------------------------------------------------------------------------

    }

    //----------------------------------------------------------------------------------------------
    //------------------------------ Boton de iniciar / parar --------------------------------------
    //----------------------------------------------------------------------------------------------
    private void accion_boton_ini_fin () {
        boolean bail = false;
        // Si estaba encendido, solo apago
        if (!stopped) {
            stopped = true;
            f_fundamental=0;
            // Actualizo Frecuencia
            final TextView mostrarFrec = findViewById(R.id.editText_frec);
            final TextView indicador = findViewById(R.id.Indicador);
            mostrarFrec.setText(f_fundamental+"Hz");
            indicador.setBackgroundColor(getResources().getColor(R.color.buttom_off));
            indicador.setText("Indicador");
            return;
        }


        // Si algo salio mal, no activo.
        if (bail) return;


        //---------------------------------- EJECUTO EL THREAD DE AUDIO RECORDER -------------------
        stopped = false;
        // Este thread va a estar siempre grabando audio
        new Thread(new Runnable() {
            @Override
            public void run() {
                        getDataAudio();
                    }
        }).start();


    }

    //----------------------------------------------------------------------------------------------
    //------------------------------ CALC FFT - JAVA -----------------------------------------------
    //----------------------------------------------------------------------------------------------
    // Este método hace la FFT
    private void calcFFT () {
        // Solo si hay nuevos datos en el buffer...
        if (buffer_ready) {

            // Pasamos a double como quiere la clase FFT
            for (int i = 0; i < BUFFER_SIZE; i++)
            {
                buffer_double[i] = buffer[i]*hanning[i]; //Ventaneo
            }

            // HAcemos la FFT. La salida va a estar en el mismo buffer. Solo saca la parte
            // real (izquierda) de la FFT, intercalando la salida real y la imaginaria.
            fft.realForward(buffer_double);


            updateFFT_values();

            // Actualizo Frecuencia
            final TextView mostrarFrec = findViewById(R.id.editText_frec);
            mostrarFrec.setText(f_fundamental+"Hz");
            try {
                //Ponemos a "Dormir" el programa durante los ms que queremos
                Thread.sleep(500);
            } catch (Exception e) {
            }



            NotaCompare();

            // Terminamos de procesar el buffer, reseteamos el flag
            buffer_ready = false;
        }
    }

    //Actualización graficos FFT
    private float buffer_aux[] = new float[BUFFER_SIZE_SHOW_FREQ];

    //Funcion que trabaja con los datos transformados
    private void updateFFT_values() {
        //En esta funcion se obtiene el modulo

        for (int i=0;i<BUFFER_SIZE_SHOW_FREQ;i++)
        {
            //calculo el modulo
            double aux_mod = sqrt(buffer_double[i]*buffer_double[i] + buffer_double[i+1]*buffer_double[i+1]);

            aux_mod = 20*log(aux_mod);
            buffer_aux[i] = (float) aux_mod;
        }

        findPeaks(); //busco puntos y se calcula la frecuencia fundamental

    }



    //Busco los picos y saco la frecuencia fundamental
    private void findPeaks() {
        int len_freq = 0;
        float buff_freq[]=new float [(int) BUFFER_SIZE_SHOW_FREQ/2];
        float Amplitud_muestras[]= new float [(int) BUFFER_SIZE_SHOW_FREQ/2];


        for(int i=1;i<buffer_aux.length-1;i++)
        {
            if( (buffer_aux[i]-buffer_aux[i-1])>0 && (buffer_aux[i+1]-buffer_aux[i])<=0 )
            {
                if(buffer_aux[i]>NIVELMINIMO-0)
                {
                    Amplitud_muestras[len_freq]=buffer_aux[i];

                    if(bufferFreq[i]>FREC_ELEGIDA-20&&bufferFreq[i]<FREC_ELEGIDA+20){
                        buff_freq[len_freq]=bufferFreq[i];
                        len_freq++;
                    }

                }
            }
        }

        // Busco el maximo de picos obtenidos (f fundamental)
        // luego busco a que frecuencia pertenece y es la que aparece por pantalla

        if(len_freq!=0)
        {

            f_fundamental =  findmax(buff_freq);//buff_freq[findmax(Amplitud_muestras)-1];
        }
    }

    //Funcion que sirve para encontrar el pico más alto despues de transformar
    //private int findmax(float aux[]){
    private float findmax(float aux[]){
        int arg=0;
        float suma=0;
        for(int i=0;i<aux.length-1;i++){
            if(aux[i]==0){
                arg=i;
                i= aux.length;
            }
            else suma=suma+aux[i];
        }
        suma=suma/arg;
        //return arg;
        return suma;
    }

    //----------------------------------------------------------------------------------------------
    //------------------------------ CAPTURAR AUDIO ------------------------------------------------
    //----------------------------------------------------------------------------------------------
    private void getDataAudio() {

        // Seteamos la prioridad
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

        // intentamos crear el grqabador de audio y grabar...

        try {

            // Empezamos a grabar
            recorder.startRecording();

            // Mientras no me digan que pare...
            while(!stopped) {
                //Log.d("PRUEBA", "Estoy grabando");

                // Leo las muestras de audio
                recorder.read(buffer,0,BUFFER_SIZE);

                // Si llego aca es que hay nueva info, seteo el flag para la FFT
                buffer_ready = true;

            }
        } catch(Throwable x) {
            Log.w("Error Audio: ","Error reading voice audio",x);
        } finally {
            //close();
        }
    }

    //Funcion que compara la fundamental con la nota calibrada y muestra si esta afinada o no
    private void NotaCompare() {
        // Actualizo Frecuencia
        final TextView indicador = findViewById(R.id.Indicador);
        boolean flag_afinado =false;
        boolean flag_abajo =false;
        boolean flag_arriba =false;

        //Mi Grave
        if(flag_miG){
            if(f_fundamental<(NOTA_MIG+DELTA) && f_fundamental>NOTA_MIG-DELTA) {
                flag_afinado=true;
            }else if(f_fundamental<(NOTA_MIG+DELTA)){
                flag_abajo=true;
            }else if(f_fundamental>NOTA_MIG-DELTA){
                flag_arriba=true;
            }
        }

        //La
        if(flag_la){
            if(f_fundamental<(NOTA_LA+DELTA) && f_fundamental>NOTA_LA-DELTA) {
                flag_afinado=true;
            }else if(f_fundamental<(NOTA_LA+DELTA)){
                flag_abajo=true;
            }else if(f_fundamental>NOTA_LA-DELTA){
                flag_arriba=true;
            }
        }

        //Re
        if(flag_re){
            if(f_fundamental<(NOTA_RE+DELTA) && f_fundamental>NOTA_RE-DELTA) {
                flag_afinado=true;
            }else if(f_fundamental<(NOTA_RE+DELTA)){
                flag_abajo=true;
            }else if(f_fundamental>NOTA_RE-DELTA){
                flag_arriba=true;
            }
        }

        //SOL
        if(flag_sol){
            if(f_fundamental<(NOTA_SOL+DELTA) && f_fundamental>NOTA_SOL-DELTA) {
                flag_afinado=true;
            }else if(f_fundamental<(NOTA_SOL+DELTA)){
                flag_abajo=true;
            }else if(f_fundamental>NOTA_SOL-DELTA){
                flag_arriba=true;
            }
        }

        //SI
        if(flag_si){
            if(f_fundamental<(NOTA_SI+DELTA) && f_fundamental>NOTA_SI-DELTA) {
                flag_afinado=true;
            }else if(f_fundamental<(NOTA_SI+DELTA)){
                flag_abajo=true;
            }else if(f_fundamental>NOTA_SI-DELTA){
                flag_arriba=true;
            }
        }
        //Mi Agudo
        if(flag_miA){
            if(f_fundamental<(NOTA_MIA+DELTA) && f_fundamental>NOTA_MIA-DELTA) {
                flag_afinado=true;
            }else if(f_fundamental<(NOTA_MIA+DELTA)){
                flag_abajo=true;
            }else if(f_fundamental>NOTA_MIA-DELTA){
                flag_arriba=true;
            }
        }


        if(flag_afinado){
            flag_afinado=false;
            indicador.setBackgroundColor(getResources().getColor(R.color.color_afinado));
            indicador.setText("Afinado");
            esperar(2);
        }
        if(flag_arriba){
            flag_arriba=false;
            indicador.setBackgroundColor(getResources().getColor(R.color.color_desafinado));
            indicador.setText("Desajustar");
            //esperar(2);
        }
        if(flag_abajo){
            flag_abajo=false;
            indicador.setBackgroundColor(getResources().getColor(R.color.color_desafinado));
            indicador.setText("Ajustar");
           //esperar(2);
        }



    }

    //Funcion para "dormir" el programa X segundos
    void esperar(int segundos){
        try{
            Thread.sleep(segundos*1000);
        }catch (Exception e){
            System.out.println(e);
        }
    }
}