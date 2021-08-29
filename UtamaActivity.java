package com.pedulisekitar.dijkstra;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.pedulisekitar.dermawan.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UtamaActivity extends FragmentActivity implements  OnMapReadyCallback, OnMapClickListener, OnMapLongClickListener {

	// DB
	SQLHelper dbHelper;
	Cursor cursor;

	// Google Maps
	private GoogleMap googleMap;

	public String __global_endposition = null;
	public String __global_startposition = null;
	public int __global_simpul_awal;
	public int __global_simpul_akhir;	
	public String __global_old_simpul_awal = "";
	public String __global_old_simpul_akhir = "";
	public int __global_maxRow0;
	public int __global_maxRow1;
	private String[][] __global_graphArray;
	private LatLng __global_yourCoordinate_exist = null;

	private Context context;

	private String latipengguna, longipengguna="";
	private Marker LOKASI;
	private LatLng koordinat_user = null;
	public String koorjson = null;

    private List<Polyline> polylinePaths = new ArrayList<>();
    public ArrayList<Polyline> polylines = new ArrayList<>();
	private Marker lokasi_saya = null , lokasi_tujuan = null, lokasi_jalan = null ;

    private double currentLatitude = 0.0;
    private double currentLongitude = 0.0;


	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_utama);

		Intent in = getIntent();
		latipengguna = in.getStringExtra("latiuser");
		longipengguna = in.getStringExtra("longiuser");
//		Toast.makeText(this, "cek latlong utama: "+latipengguna+longipengguna, Toast.LENGTH_SHORT).show();


		context = this;
		// create DB
		dbHelper = new SQLHelper(this);
        try {
        	dbHelper.CopyAndCreateDataBase();
        } 
        catch (Exception ioe) {
        	Toast.makeText(getApplicationContext(), "Gagal", Toast.LENGTH_LONG).show();
        }
 		
//         BUAT MAP

			SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
					.findFragmentById(R.id.peta);
			mapFragment.getMapAsync(this);
			if(mapFragment!=null) {
				mapFragment.getMapAsync(new OnMapReadyCallback() {
					@Override
					public void onMapReady(GoogleMap googleMap) {
						MapsInitializer.initialize(context);
						setupMap(googleMap);

						// event map
						googleMap.setOnMapClickListener((OnMapClickListener) context);
						googleMap.setOnMapLongClickListener((OnMapLongClickListener) context);


					}
				});
			}


        
		// Query DB to show all Yayasan
		dbHelper = new SQLHelper(this);
		final SQLiteDatabase db = dbHelper.getReadableDatabase();		
		cursor = db.rawQuery("SELECT * FROM yayasan", null);
		cursor.moveToFirst();
		
		// tampung nama Yayasan
		ArrayList<String> spinner_list_smk = new ArrayList<String>();		
		// Adapter spinner Yayasan
		ArrayAdapter<String> adapter_spinner_smk;	
		
		// nama-nama Yayasan dimasukkan ke array
		spinner_list_smk.add("-- Pilih Wilayah Tujuan --");
		for(int i = 0; i < cursor.getCount(); i++){
			cursor.moveToPosition(i);
			spinner_list_smk.add(cursor.getString(1).toString());
		}
		
		// masukkan list Yayasan ke spinner (dropdown)
		Spinner spinner = (Spinner) findViewById(R.id.spinner_list_smk);	
	    adapter_spinner_smk = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, spinner_list_smk);
		adapter_spinner_smk.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);	
		spinner.setAdapter(adapter_spinner_smk);
		spinner.setBackgroundColor(Color.WHITE);


		
		spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				// TODO Auto-generated method stub
	
				if(arg0.getItemAtPosition(arg2).toString() != "-- Pilih Wilayah Tujuan  --"){

	 				String pilih_smk = arg0.getItemAtPosition(arg2).toString();
					Log.d("DEBUGGING", "DEBUGGING: 01" +"Yayasan terpilih"+pilih_smk);
	 				cursor = db.rawQuery("SELECT koordinat FROM yayasan where tujuan = '" + pilih_smk + "'", null);
	 				cursor.moveToFirst();
//					cursor.moveToPosition(0);
					// 3. if we got results get the first one
					if (cursor.moveToFirst()){
                        Log.d("DEBUGGING", "DEBUGGING: 03");
						cursor.moveToPosition(0);

						// get coordinate Yayasan from field koordinat
						__global_endposition = cursor.getString(0).toString();

					}
//					__global_endposition = cursor.getString(0).toString();
                    Log.d("DEBUGGING", "DEBUGGING: 04"+"Koordinat Yayasan terpilih"+__global_endposition);
	 				// user men-tap peta
	 				if(__global_yourCoordinate_exist != null){

	 					// your coordinate
                        double latUser=0, lngUser=0;
	 					 latUser = __global_yourCoordinate_exist.latitude;
                        lngUser = __global_yourCoordinate_exist.longitude;

                        Log.d("DEBUGGING", "DEBUGGING: 05"+"Latlonguser tab peta "+latUser+lngUser);


                        // destination coordinate Yayasan
	 					String[] exp_endCoordinate = __global_endposition.split(",");
	 					double lat_endposition = Double.parseDouble(exp_endCoordinate[0]);
	 					double lng_endposition = Double.parseDouble(exp_endCoordinate[1]);

                        Log.d("DEBUGGING", "DEBUGGING: 06"+"latlong koor yayasan"+lat_endposition+lng_endposition);


//                        Toast.makeText(context, "latuser"+latUser+lngUser+"/n"+"lat tujuan"+lat_endposition+lng_endposition, Toast.LENGTH_LONG).show();


						// ========================================================================
	 					// CORE SCRIPT
	 					// fungsi cari simpul awal dan tujuan, buat graph sampai algoritma dijkstra
	 					// ========================================================================
	 					try {

                            Log.d("DEBUGGING", "DEBUGGING: 07"+latUser+lngUser+lat_endposition+ lng_endposition);
 	 						startingScript(latUser, lngUser, lat_endposition, lng_endposition);
                            Log.d("DEBUGGING", "DEBUGGING: 09");
	 					} catch (JSONException e) {
	 						// TODO Auto-generated catch block
	 						e.printStackTrace();
                            Log.d("DEBUGGING", "DEBUGGING: 08");
	 					}
	 					
	 				}else{
                        Log.d("DEBUGGING", "DEBUGGING: 02");
	 					Toast.makeText(getApplicationContext(), "Tap pada peta untuk menentukan posisi Anda", Toast.LENGTH_LONG).show();
	 				}

				}// if -- pilih Yayasan --
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// TODO Auto-generated method stub
				
			}
		    	
	    });//setOnItemSelectedListener
	
	}

	@Override
	public void onMapLongClick(LatLng arg0) {
		// TODO Auto-generated method stub
		
	}

	
	
	
	@Override
	public void onMapClick(LatLng arg0) {

//        clearMap(true);
		// your coordinate position
		double latUser = arg0.latitude;
		double lngUser = arg0.longitude;
		
		__global_yourCoordinate_exist = arg0;
		
		// destination coordinate position
		String endposition = __global_endposition;
        Log.d("DEBUGGING", "DEBUGGING: 20"+endposition+__global_endposition);
		if(endposition != null){

			// dipecah coordinate Yayasan
			String[] exp_endposition = endposition.split(",");
			double lat_endposition = Double.parseDouble(exp_endposition[0]);
			double lng_endposition = Double.parseDouble(exp_endposition[1]);
            Log.d("DEBUGGING", "DEBUGGING: 22"+exp_endposition+lat_endposition+lng_endposition);
//			Toast.makeText(getApplicationContext(), "latuser"+latUser+"longuser"+lngUser+"/n"+"lat tujuan"+lat_endposition+"long tujuan"+lng_endposition, Toast.LENGTH_LONG).show();

			// ========================================================================
			// CORE SCRIPT 
			// fungsi cari simpul awal dan tujuan, buat graph sampai algoritma dijkstra
			// ========================================================================
			try {
                Log.d("DEBUGGING", "DEBUGGING: 23");
				startingScript(latUser, lngUser, lat_endposition, lng_endposition);

			} catch (JSONException e) {
                Log.d("DEBUGGING", "DEBUGGING: 24");
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}else{
            Log.d("DEBUGGING", "DEBUGGING: 21");
			Toast.makeText(getApplicationContext(), "pilih yayasan tujuan dahulu", Toast.LENGTH_LONG).show();
		}
		
	}

	public void clearMap(Boolean marker){
		if(!polylines.isEmpty()){
			for(Polyline pol:polylines){
				pol.remove();
			}
            polylines.clear();
		}

			if(lokasi_saya!=null && lokasi_jalan!=null && lokasi_tujuan!=null){
                lokasi_saya.remove();
				lokasi_jalan.remove();
				lokasi_tujuan.remove();

			}

//            if(lokasi_jalan!=null){
//                lokasi_jalan.remove();
//            }
//
//            if(lokasi_tujuan!=null){
//                lokasi_tujuan.remove();
//            }



	}



	/*
	 * ===========
	 * CORE SCRIPT
	 * ===========
	 * 
	 * @fungsi utama
	 *  (1) mendapatkan koordinat awal dan akhir di sekitar jalur angkutan umum
	 *  
	 *  (2) koordinat awal kemudian di konversi menjadi simpul awal
	 *      dan koordinat akhir di konversi menjadi simpul akhir
	 *      
	 *  (3) simpul awal dan akhir kemudian dijadikan 'inputan' untuk perhitungan algoritma dijsktra
	 *  
	 *  (4) setelah dilakukan perhitungan, didapatkan jalur terpendek lalu digambar jalurnya menggunakan polyline
	 *
	 * @parameter
	 *  latUser dan lngUser : koordinat posisi user
	 *  lat_endposition dan lng_endposition : koordinat posisi Yayasan
	 * 
	 * @return
	 *  no return
	 */
	public void startingScript(double latUser, double lngUser, double lat_endposition, double lng_endposition) throws JSONException{				
		    	
		// delete temporary record DB
		deleteTemporaryRecord();

        clearMap(true);
		// reset google map
//		googleMap.clear();

		// convert graph from DB to Array; graph[][]
		GraphToArray DBGraph = new GraphToArray();
		__global_graphArray = DBGraph.convertToArray(this); // return graph[][] Array
		
		// get max++ row temporary DB
		maxRowDB();
		
		// GET COORDINATE AWAL DI SEKITAR SIMPUL
		// coordinate awal lalu di konversi ke simpul awal
		// return __global_simpul_awal, __global_graphArray[][]
		// ==========================================
		Get_koordinat_awal_akhir start_coordinate_jalur = new Get_koordinat_awal_akhir();
		getSimpulAwalAkhirJalur(start_coordinate_jalur, latUser, lngUser, "awal");

		// GET COORDINATE AKHIR DI SEKITAR SIMPUL
		// coordinate akhir lalu di konversi ke simpul akhir
		// return __global_simpul_akhir, __global_graphArray[][]
		// ==========================================
		Get_koordinat_awal_akhir destination_coordinate_jalur = new Get_koordinat_awal_akhir();		
		getSimpulAwalAkhirJalur(destination_coordinate_jalur, lat_endposition, lng_endposition, "akhir");

		// ALGORITMA DIJKSTRA
		// ==========================================
		dijkstra algo = new dijkstra();
		algo.jalurTerpendek(__global_graphArray, __global_simpul_awal, __global_simpul_akhir);
		Log.d("DEBUGGING", "DEBUGGING: 1ga "+__global_graphArray);
        Log.d("DEBUGGING", "DEBUGGING: 1sa "+__global_simpul_awal);
        Log.d("DEBUGGING", "DEBUGGING: 1st "+__global_simpul_akhir);

		// no result for algoritma dijkstra
		if(algo.status == "die"){
			Log.d("DEBUGGING", "DEBUGGING: 2");
			Toast.makeText(getApplicationContext(), "Lokasi Anda sudah dekat dengan lokasi tujuan", Toast.LENGTH_LONG).show();
		
		}else{
			// return jalur terpendek; example 1->5->6->7
	       	String[] exp = algo.jalur_terpendek1.split("->");
			Log.d("DEBUGGING", "DEBUGGING: 3"+exp);
            Toast.makeText(context, "Jalur Terpendek =  "+algo.jalur_terpendek1, Toast.LENGTH_LONG).show();
	       	// DRAW JALUR ANGKUTAN UMUM
			// =========================================

	       	drawJalur(algo.jalur_terpendek1, exp);
			Log.d("DEBUGGING", "DEBUGGING: 4");
		}

	}


	
	/*
	 * @fungsi
	 *  menggambar jalur angkutan umum
	 *  menentukan jenis angkutan umum yang melewati jalur tsb
	 *  membuat marker untuk your position dan destination position
	 * @parameter
	 *  exp[] : jalur terpendek; example 1->5->6->7
	 * @return
	 *  no return
	 */
	public void drawJalur(String alg, String[] exp) throws JSONException{
		
        int start = 0;
		
        // GAMBAR JALURNYA
        // ======================
		dbHelper = new SQLHelper(this);
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		Log.d("DEBUGGING", "DEBUGGING: 5");
        for(int i = 0; i < exp.length-1; i++){
        
        	ArrayList<LatLng> lat_lng = new ArrayList<LatLng>();
        	
        	cursor = db.rawQuery("SELECT jalur FROM graph where simpul_awal ="+exp[start]+" and simpul_tujuan ="+exp[(++start)], null);
			cursor.moveToFirst();
			Log.d("DEBUGGING", "DEBUGGING: 6");

			
			// dapatkan koordinat Lat,Lng dari field koordinat (3)
			String json = cursor.getString(0).toString();
//            Toast.makeText(getApplicationContext(), "lat long koordinat"+json, Toast.LENGTH_LONG).show();


            // get JSON
			JSONObject jObject = new JSONObject(json);
			JSONArray jArrCoordinates = jObject.getJSONArray("coordinates");

			// get coordinate JSON
			for(int w = 0; w < jArrCoordinates.length(); w++){
				
				JSONArray latlngs = jArrCoordinates.getJSONArray(w);
				Double lats = latlngs.getDouble(0);
				Double lngs = latlngs.getDouble(1);


				lat_lng.add( new LatLng(lats, lngs) );


			}
			Log.d("DEBUGGING", "DEBUGGING: 10"+lat_lng);
			// buat rute


			Log.d("DEBUGGING", "DEBUGGING: 12");
                polylines.add(
                        googleMap.addPolyline(new PolylineOptions()
                                .addAll(lat_lng)
                                .width(15).color(Color.BLUE).geodesic(true)));

			Log.d("DEBUGGING", "DEBUGGING: 13");
        }
        
        
        // BUAT MARKER UNTUK YOUR POSITION AND DESTINATION POSITION
        // ======================
        // your position
		Log.d("DEBUGGING", "DEBUGGING: 15"+__global_yourCoordinate_exist);

        lokasi_saya = googleMap.addMarker(new MarkerOptions()
					.position(__global_yourCoordinate_exist)
					.title("Lokasi Saya")
					.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
//
		String[] exp_endCoordinate = __global_endposition.split(",");
		double lat_endPosition = Double.parseDouble(exp_endCoordinate[0]);
		double lng_endPosition = Double.parseDouble(exp_endCoordinate[1]);		
		LatLng endx = new LatLng(lat_endPosition, lng_endPosition);

		Log.d("DEBUGGING", "DEBUGGING: 16"+endx);
        
		// destination position

        lokasi_tujuan = googleMap.addMarker(new MarkerOptions()
					.position(endx)
					.title("Lokasi Yayasan")
					.icon(BitmapDescriptorFactory.fromResource(R.drawable.pedulisekitarlogo)));

		
        // TENTUKAN JENIS ANGKUTAN UMUM YANG MELEWATI JALUR TERSEBUT
        // ==========================================================
		// misal exp[] = 1->5->6->7
		int m = 0;
		
		
		String[] awal = __global_old_simpul_awal.split("-"); // misal 4-5
		String[] akhir = __global_old_simpul_akhir.split("-"); // misal 8-7

		int ganti_a = 0;
		int ganti_b = 0;
		String simpulAwalDijkstra = exp[0];


		String gabungSimpul_all = "";
    	Map<String, ArrayList> listAngkutanUmum = new HashMap<String, ArrayList>();
    	ArrayList<Integer> listSimpulAngkot = new ArrayList<Integer>();

    	// cari simpul_old sebelum koordinat dipecah
    	// misal 4-5 dipecah menjadi 4-6-5, berarti simpul_old awal = 5, simpul_old akhir = 4
        for(int e = 0; e < (exp.length - 1); e++){
        	
        	if(e == 0){ // awal

        		// dijalankan jika hasil algo hanya 2 simpul, example : 4->5
        		if(exp.length == 2 /* 2 simpul (4-5)*/){
            		
        			// ada simpul baru di awal (10) dan di akhir (11), example 10->11
        			if( exp[0].equals(String.valueOf(__global_maxRow0)) && exp[1].equals(String.valueOf(__global_maxRow1)) ){				
        				
    					if(String.valueOf(__global_maxRow0).equals(akhir[0])){
    						ganti_b = Integer.parseInt(akhir[1]);
    					}else{
    						ganti_b = Integer.parseInt(akhir[0]);
    					}
    					
    					if(String.valueOf(ganti_b).equals(awal[0])){
    						ganti_a = Integer.parseInt(awal[1]);
    					}else{
    						ganti_a = Integer.parseInt(awal[0]);
    					}
        			}
        			else{
        				// ada simpul baru di awal (10), example 10->5
        				// maka cari simpul awal yg oldnya
        				if( exp[0].equals(String.valueOf(__global_maxRow0)) ){
        					
            				if(exp[1].equals(awal[1])){
	        					ganti_a = Integer.parseInt(awal[0]);
            				}else{
            					ganti_a = Integer.parseInt(awal[1]);
            				}
	            				ganti_b = Integer.parseInt(exp[1]);
        				}
        				// ada simpul baru di akhir (10), example 5->10
        				// maka cari simpul akhir yg oldnya
        				else if( exp[1].equals(String.valueOf(__global_maxRow0)) ){
        					
        					if(exp[0].equals(akhir[0])){
	        					ganti_b = Integer.parseInt(akhir[1]);
            				}else{
            					ganti_b = Integer.parseInt(akhir[0]);
            				}       					
        					ganti_a = Integer.parseInt(exp[0]);   					
        				}
        				// tidak ada penambahan simpul sama sekali
        				else{
        					ganti_a = Integer.parseInt(exp[0]);
        					ganti_b = Integer.parseInt(exp[1]);
        				}
        			}
        			
        			/*
        			// 4 == 4
        			if(exp[0].equals(awal[0])){
            			ganti_a = Integer.parseInt(awal[0]);
            			//ganti_b = Integer.parseInt(awal[1]);      				
        			}else{
            			ganti_a = Integer.parseInt(awal[1]);
            			//ganti_b = Integer.parseInt(awal[0]);       				
        			}
        			
        			if(String.valueOf(ganti_a).equals(akhir[0])){
            			ganti_b = Integer.parseInt(akhir[1]);
            			//ganti_b = Integer.parseInt(awal[1]);      				
        			}else{
            			ganti_b = Integer.parseInt(akhir[0]);
            			//ganti_b = Integer.parseInt(awal[0]);       				
        			}
        			*/
        			
        			/*
        			 *         			// 4 == 4
        			if(exp[0].equals(awal[0])){
            			ganti_a = Integer.parseInt(akhir[0]);
            			ganti_b = Integer.parseInt(awal[1]);      				
        			}else{
            			ganti_a = Integer.parseInt(awal[1]);
            			ganti_b = Integer.parseInt(akhir[0]);       				
        			}
        			 */
        			
        		}
        		// hasil algo lebih dr 2 : 4->5->8->7-> etc ..
        		else{        			
            		if(exp[1].equals(awal[1])){ // 5 == 5
            			ganti_a = Integer.parseInt(awal[0]); // hasil 4
            		}else{
            			ganti_a = Integer.parseInt(awal[1]); // hasil 5
            		}
            		
        			ganti_b = Integer.parseInt( exp[++m] );
        		}
        	}	        
        	else if(e == (exp.length - 2)){ // akhir
        		
        		if(exp[ (exp.length - 2) ].equals(akhir[1])){ // 7 == 7
        			ganti_b = Integer.parseInt(akhir[0]); // hasil 8
        		}else{
        			ganti_b = Integer.parseInt(akhir[1]); // hasil 7
        		}
        		
        		ganti_a = Integer.parseInt( exp[m] );
        		
        	}else{ // tengah tengah
        		ganti_a = Integer.parseInt( exp[m] );
        		ganti_b = Integer.parseInt( exp[++m] );
        	}

        	gabungSimpul_all += "," + ganti_a + "-" + ganti_b + ","; // ,1-5,
        	String gabungSimpul = "," + ganti_a + "-" + ganti_b + ","; // ,1-5,
        	
			cursor = db.rawQuery("SELECT * FROM jalur_mobil where simpul like '%" + gabungSimpul + "%'", null);
			cursor.moveToFirst();

			ArrayList<String> listAngkutan = new ArrayList<String>();
			
			for(int ae = 0; ae < cursor.getCount(); ae++){				
				cursor.moveToPosition(ae);
				listAngkutan.add( cursor.getString(1).toString() );				
			}        	
        	
			listAngkutanUmum.put("angkutan" + e, listAngkutan);
			
			// add simpul angkot
			listSimpulAngkot.add( Integer.parseInt(exp[e]) ); 

        }
 
		
        String replace_jalur = gabungSimpul_all.replace(",,", ","); //  ,1-5,,5-6,,6-7, => ,1-5,5-6,6-7,
		cursor = db.rawQuery("SELECT * FROM jalur_mobil where simpul like '%" + replace_jalur + "%'", null);
		cursor.moveToFirst();
		cursor.moveToPosition(0);
		
		// ada 1 angkot yg melewati jalur dari awal sampek akhir
		if(cursor.getCount() > 0){

			String siAngkot = cursor.getString(1).toString();

			// get coordinate
			cursor = db.rawQuery("SELECT jalur FROM graph where simpul_awal = '" + simpulAwalDijkstra + "'", null);
			cursor.moveToFirst();
//			cursor.moveToPosition(0);

			Log.d("DEBUGGING Count",String.valueOf(cursor.getCount()));
			if (cursor.moveToFirst()){
                cursor.moveToPosition(0);
//
//
////                String json = cursor.getString(0).toString();
//				Log.d("DEBUGGING", "DEBUGGING: 7");
//
            }
            // dapatkan koordinat Lat,Lng dari field koordinat (3)
			String json_coordinate = cursor.getString(0).toString();
			Log.d("DEBUGGING", "DEBUGGING: 17"+json_coordinate);

			// manipulating JSON
			JSONObject jObject = new JSONObject(json_coordinate);
			JSONArray jArrCoordinates = jObject.getJSONArray("coordinates");
			JSONArray latlngs = jArrCoordinates.getJSONArray(0);
			Double lats = latlngs.getDouble(0);
			Double lngs = latlngs.getDouble(1);

			Log.d("DEBUGGING", "jalur"+latlngs);

            lokasi_jalan = googleMap.addMarker(new MarkerOptions()
                        .position(new LatLng(lats, lngs))
                        .title("Jalan")
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.jalan)));



			// first latlng

			// die()
			return;
		}
		
		// ada 2 atau lebih angkot yg melewati jalur dari awal sampek akhir
		int banyakAngkot = 0;
		int indexUrut = 0;
		int indexSimpulAngkot = 1;
        int lengthAngkutan = listAngkutanUmum.size();
        Map<String, ArrayList> angkotFix = new HashMap<String, ArrayList>();

        for(int en = 0; en < lengthAngkutan; en++ ){

        	// temporary sementara sebelum di retainAll()
        	ArrayList<String> temps = new ArrayList<String>();
        	for(int u = 0; u < listAngkutanUmum.get("angkutan0").size(); u++){
        		temps.add( listAngkutanUmum.get("angkutan0").get(u).toString() );
        	}        	        	
        	
        	if(en > 0 ){
	    		ArrayList listSekarang1 = listAngkutanUmum.get("angkutan0");
				ArrayList listSelanjutnya1 = listAngkutanUmum.get("angkutan" + en);	
				
				// intersection
				listSekarang1.retainAll(listSelanjutnya1);
	            
	            if(listSekarang1.size() > 0){
	            	
	            	listSimpulAngkot.remove(indexSimpulAngkot);
	            	--indexSimpulAngkot;

	            	listAngkutanUmum.remove("angkutan" + en);

	            	if(en == (lengthAngkutan - 1)){
	            		
		            	ArrayList<String> tempDalam = new ArrayList<String>();
		            	for(int es = 0; es < listSekarang1.size(); es++){
		            		tempDalam.add( listSekarang1.get(es).toString() );
		            	}
		            	
	            		angkotFix.put("angkutanFix" + indexUrut, tempDalam);
		            	++indexUrut;	
	            	}
	            }	            
	            else if(listSekarang1.size() == 0){
	            	
	            	angkotFix.put("angkutanFix" + indexUrut, temps);
	            	
	            	ArrayList<String> tempDalam = new ArrayList<String>();
	            	for(int es = 0; es < listSelanjutnya1.size(); es++){
	            		tempDalam.add( listSelanjutnya1.get(es).toString() );
	            	}
	            	
	            	//if(en == 1) break;
	            	listAngkutanUmum.get("angkutan0").clear();
	            	listAngkutanUmum.put("angkutan0", tempDalam);
	            	
	            	//if(en != (listAngkutanUmum.size() - 1)){
	            		listAngkutanUmum.remove("angkutan" + en);	
	            	//}
	            	
		            ++indexUrut;
		            
	            	if(en == (lengthAngkutan - 1)){
	            		
		            	ArrayList<String> tempDalam2 = new ArrayList<String>();
		            	for(int es = 0; es < listSelanjutnya1.size(); es++){
		            		tempDalam2.add( listSelanjutnya1.get(es).toString() );
		            	}
		            	
	            		angkotFix.put("angkutanFix" + indexUrut, tempDalam2);
		            	++indexUrut;	
	            	}		            
	            }
	        	
	        	++indexSimpulAngkot;
        	}
        }
        
        for(int r = 0; r < listSimpulAngkot.size(); r++){
        	String simpulx = listSimpulAngkot.get(r).toString();
			// get coordinate simpulAngkutan
			cursor = db.rawQuery("SELECT jalur FROM graph where simpul_awal = '" + simpulx + "'", null);
			cursor.moveToPosition(0);
			
			// dapatkan koordinat Lat,Lng dari field koordinat (3)
			String json = cursor.getString(0).toString();

			// get JSON
			JSONObject jObject = new JSONObject(json);
			JSONArray jArrCoordinates = jObject.getJSONArray("coordinates");

			// get first coordinate JSON
			JSONArray latlngs = jArrCoordinates.getJSONArray(0);
			Double lats = latlngs.getDouble(0);
			Double lngs = latlngs.getDouble(1);
				
			LatLng simpulAngkot = new LatLng(lats, lngs);
			String siAngkot = angkotFix.get("angkutanFix" + r).toString();
			
			if(r == 0){

                lokasi_jalan = googleMap.addMarker(new MarkerOptions()
                                    .position(simpulAngkot)
                                    .title("Jalan")
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.jalan)));

			}else{

                lokasi_jalan = googleMap.addMarker(new MarkerOptions()
                            .position(simpulAngkot)
                            .title("Jalan")
//							.snippet("")
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.jalan)));

			}
        }
        
	}
	
	public void getSimpulAwalAkhirJalur(Get_koordinat_awal_akhir objects, double latx, double lngx, String statusObject) throws JSONException{
		
		// return JSON index posisi koordinat, nodes0, nodes1
		JSONObject jStart = objects.Get_simpul(latx, lngx, this);

		// index JSON
		String status = jStart.getString("status");
		int node_simpul_awal0 = jStart.getInt("node_simpul_awal0");
		int node_simpul_awal1 = jStart.getInt("node_simpul_awal1");
		int index_coordinate_json = jStart.getInt("index_coordinate_json");
		
		
		int fix_simpul_awal = 0;
		
		// jika koordinat tepat di atas posisi simpul/node
		// maka tidak perlu menambahkan simpul baru
		if(status.equals("jalur_none")){
			
			//tentukan simpul awal atau akhir yg dekat dgn posisi user
			if(index_coordinate_json == 0){ // awal			
				fix_simpul_awal = node_simpul_awal0;				
			}else{ // akhir				
				fix_simpul_awal = node_simpul_awal1;				
			}
			
			if(statusObject == "awal"){	
				
				// return
				__global_old_simpul_awal = node_simpul_awal0 + "-" + node_simpul_awal1;
				__global_simpul_awal = fix_simpul_awal; // misal 0				
			}else{
				
				// return
				__global_old_simpul_akhir = node_simpul_awal0 + "-" + node_simpul_awal1;
				__global_simpul_akhir = fix_simpul_awal; // misal 0				
			}
		
						
		}
		// jika koordinat berada diantara simpul 5 dan simpul 4 atau simpul 4 dan simpul 5
		// maka perlu menambahkan simpul baru
		else if(status.equals("jalur_double")){

			// return		
			if(statusObject == "awal"){				
				
				// cari simpul (5,4) dan (4-5) di Tambah_simpul.java
				Tambah_simpul obj_tambah = new Tambah_simpul();
				obj_tambah.dobelSimpul(node_simpul_awal0, node_simpul_awal1, index_coordinate_json, 
										this, __global_graphArray, 401
									); // 401 : row id yg baru
										
				
				// return
				__global_old_simpul_awal = obj_tambah.simpul_lama;
				__global_simpul_awal = obj_tambah.simpul_baru; // misal 6
				__global_graphArray = obj_tambah.modif_graph; // graph[][]

			}else{
			
				// cari simpul (5,4) dan (4-5) di Tambah_simpul.java
				Tambah_simpul obj_tambah = new Tambah_simpul();
				obj_tambah.dobelSimpul(node_simpul_awal0, node_simpul_awal1, index_coordinate_json, 
										this, __global_graphArray, 501
									); // 501 : row id yg baru
										
				
				// return
				__global_old_simpul_akhir = obj_tambah.simpul_lama;
				__global_simpul_akhir = obj_tambah.simpul_baru; // misal 4			
				__global_graphArray = obj_tambah.modif_graph; // graph[][]
								
			}

		}
		// jika koordinat hanya berada diantara simpul 5 dan simpul 4
		// maka perlu menambahkan simpul baru
		else if(status.equals("jalur_single")){

			if(statusObject == "awal"){
				
				// cari simpul (5,4) di Tambah_simpul.java
				Tambah_simpul obj_tambah1 = new Tambah_simpul();
				obj_tambah1.singleSimpul(node_simpul_awal0, node_simpul_awal1, index_coordinate_json, 
										this, __global_graphArray, 401
									); // 401 : row id yg baru
										
				
				// return
				__global_old_simpul_awal = obj_tambah1.simpul_lama;
				__global_simpul_awal = obj_tambah1.simpul_baru; // misal 6
				__global_graphArray = obj_tambah1.modif_graph; // graph[][]
				
			}else{
				
				// cari simpul (5,4) di Tambah_simpul.java
				Tambah_simpul obj_tambah1 = new Tambah_simpul();
				obj_tambah1.singleSimpul(node_simpul_awal0, node_simpul_awal1, index_coordinate_json, 
						this, __global_graphArray, 501
					); // 501 : row id yg baru

				
				// return
				__global_old_simpul_akhir = obj_tambah1.simpul_lama;
				__global_simpul_akhir = obj_tambah1.simpul_baru; // misal 4			
				__global_graphArray = obj_tambah1.modif_graph; // graph[][]	
			}		
		}		
	}
	
	
	/*
	 * @fungsi
	 *  delete temporary record DB
	 *  (temporary ini digunakan untuk menampung sementara simpul baru)
	 * @parameter
	 *  no parameter
	 * @return
	 *  no returen
	 */
	public void deleteTemporaryRecord(){
		
		// delete DB
		final SQLiteDatabase dbDelete = dbHelper.getWritableDatabase();

		// delete temporary record DB
		for(int i = 0; i < 4; i++){								
			//hapus simpul awal tambahan, mulai dr id 401,402,403,404
			String deleteQuery_ = "DELETE FROM graph where id ='"+ (401+i) +"'";
			dbDelete.execSQL(deleteQuery_);	
			
			//hapus simpul tujuan tambahan, mulai dr id 501,502,503,504
			String deleteQuery = "DELETE FROM graph where id ='"+ (501+i) +"'";
			dbDelete.execSQL(deleteQuery);	
		}
	}
	
	public void maxRowDB(){
		
		dbHelper = new SQLHelper(this);
		SQLiteDatabase dbRead = dbHelper.getReadableDatabase();		
		
		cursor = dbRead.rawQuery("SELECT max(simpul_awal), max(simpul_tujuan) FROM graph", null);
		cursor.moveToFirst();
		int max_simpul_db		= 0;
		int max_simpulAwal_db 	= Integer.parseInt(cursor.getString(0).toString());			
		int max_simpulTujuan_db = Integer.parseInt(cursor.getString(1).toString());
		
		if(max_simpulAwal_db >= max_simpulTujuan_db){
			max_simpul_db = max_simpulAwal_db;
		}else{
			max_simpul_db = max_simpulTujuan_db;
		}
		
		// return
		__global_maxRow0 = (max_simpul_db+1);
		__global_maxRow1 = (max_simpul_db+2);
	}

	@Override
	public void onMapReady(GoogleMap googleMap) {
		MapsInitializer.initialize(context);
		setupMap(googleMap);

	}

	private void setupMap(GoogleMap googleMaps) {
        googleMap = googleMaps;
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(Double.parseDouble(latipengguna), Double.parseDouble(longipengguna)), 15));
//		googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(-6.2920222, 106.8774828), 12.0f));
//        googleMap.animateCamera(CameraUpdateFactory.zoomTo(15), 2000, null);
//        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
		//map.setInfoWindowAdapter(new CustomInfoWindowMaps(getLayoutInflater()));
		//map.getUiSettings().setCompassEnabled(false);
		//map.getUiSettings().setTiltGesturesEnabled(true);
		//map.getUiSettings().setRotateGesturesEnabled(false);
		//map.getUiSettings().setScrollGesturesEnabled(true);

//        googleMap.getUiSettings().setZoomControlsEnabled(true);
//        googleMap.getUiSettings().setZoomGesturesEnabled(true);
//        googleMap.getUiSettings().setMyLocationButtonEnabled(false);
//        googleMap.getUiSettings().setMapToolbarEnabled(false);
//        googleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
//            @Override
//            public boolean onMarkerClick(Marker marker) {
//                if (!(marker.getTitle().equalsIgnoreCase("LOKASI SAYA"))) {
//
//                    Intent i = new Intent(context, ActivityViewYayasan.class);
//                    i.putExtra("id_yayasan", marker.getSnippet());
//                    startActivity(i);
//                }
//                return false;
//            }
//        });
//		koordinat_user = new LatLng(Double.parseDouble(latipengguna), Double.parseDouble(longipengguna));
//
//		if (koordinat_user != null) {
//			googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(koordinat_user, 15));
//			if (LOKASI != null) {
//				LOKASI.remove();
//			}
//			LOKASI = googleMap.addMarker(new MarkerOptions()
//					.title("Lokasi Pengguna")
//					.position(koordinat_user));
//		}

	}
}
