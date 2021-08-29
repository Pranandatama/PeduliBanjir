package com.pedulisekitar.dijkstra;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static android.content.ContentValues.TAG;


public class SQLHelper extends SQLiteOpenHelper{

	private static final String DATABASE_NAME = "dbrevisi10n3.sqlite";
//	private static final int DATABASE_VERSION = 1;
	private static String DB_PATH = "";
	private Context myContext;

	public SQLHelper(Context context) {
		super(context, DATABASE_NAME, null, 3);
		myContext = context;

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
			DB_PATH = context.getApplicationInfo().dataDir + "/databases/";
		} else {
			DB_PATH = "/data/data/" + context.getPackageName() + "/databases/";
		}

		new Handler().post(new Runnable() {

			@Override
			public void run() {
				// If u want to Copy Database from Assets.
				try {
					CopyAndCreateDataBase();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
	}


	// If database not exists copy it from the assets
	public void CopyAndCreateDataBase() throws IOException {

		boolean mDataBaseExist = checkDataBase();
		if (!mDataBaseExist) {
			this.getReadableDatabase();
			this.getWritableDatabase();
			this.close();
			try {
				// Copy the database from assests
				copyDataBase();

				String mPath = DB_PATH + DATABASE_NAME;

				Log.v(TAG, "Database created :  " + mPath);

			} catch (IOException mIOException) {
				throw new Error("ErrorCopyingDataBase");
			}
		}
	}


	// Check that the database exists here: /data/data/yourpackage/databases/DatabaseName
	private boolean checkDataBase() {
		File dbFile = new File(DB_PATH + DATABASE_NAME);
		// Log.v("dbFile", dbFile + "   "+ dbFile.exists());
		return dbFile.exists();
	}

	// Copy the database from assets
	private void copyDataBase() throws IOException {
		InputStream mInput = myContext.getAssets().open(DATABASE_NAME);
		String outFileName = DB_PATH + DATABASE_NAME;
		OutputStream mOutput = new FileOutputStream(outFileName);
		byte[] mBuffer = new byte[1024];
		int mLength;
		while ((mLength = mInput.read(mBuffer)) > 0) {
			mOutput.write(mBuffer, 0, mLength);
		}
		mOutput.flush();
		mOutput.close();
		mInput.close();
	}

	@Override
	public void onCreate(SQLiteDatabase db) {

	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

	}
}