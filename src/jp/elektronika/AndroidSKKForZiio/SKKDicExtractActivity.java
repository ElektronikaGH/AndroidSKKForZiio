package jp.elektronika.AndroidSKKForZiio;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class SKKDicExtractActivity extends Activity {
	ProgressBar mProgressBar;
	Button mButton;
	UnzipTask mTask;
	private static final String ZIPFILE = "skk_dict_btree_db.zip";

	@Override public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.dic_extract);

		mTask = new UnzipTask();
		mProgressBar = (ProgressBar)findViewById(R.id.ProgressBar);
		mButton = (Button)findViewById(R.id.Button);
		mButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				mTask.cancel(true);
				dialogDone(false);
			}
		});

		mTask.execute();
	}

	private void dialogDone(boolean finished) {
		mProgressBar.setVisibility(View.GONE);

		TextView tv = (TextView)findViewById(R.id.Message);
		if (finished) {
			tv.setText(getString(R.string.message_finished));
		} else {
			tv.setText(getString(R.string.message_cancelled));
		}

		mButton.setText(getString(R.string.label_OK));
		mButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				finish();
			}
		});
	}

	private class UnzipTask extends AsyncTask<Void, Integer, Boolean> {
		@Override
		protected void onPreExecute() {
		}

		@Override
		protected Boolean doInBackground(Void ... params) {
			try {
				InputStream	is = getResources().getAssets().open(ZIPFILE);
				ZipInputStream zis = new ZipInputStream(new BufferedInputStream(is));
				ZipEntry ze = zis.getNextEntry();
				BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(new File(getFilesDir(), ze.getName())));
				int extractedSize = new Long(ze.getSize()).intValue();
				byte[] buf = new byte[1024];
				int size = 0;
				int size_done = 0;

				while ((size = zis.read(buf, 0, buf.length)) > -1) {
					if (isCancelled()) {
						bos.close();
						zis.closeEntry();
						zis.close();
						return false;
					}
					bos.write(buf, 0, size);
					size_done += size;
					publishProgress(Integer.valueOf(size_done*100/extractedSize));
				}

				bos.close();
				zis.closeEntry();
				zis.close();
			} catch (IOException e) {
				Log.e("SKK", "I/O error in extracting dictionary files");
				Log.e("SKK", e.toString());
				return false;
			}

			return true;
		}

		@Override
		protected void onProgressUpdate(Integer ... progress) {
			mProgressBar.setProgress(progress[0]);
		}

		@Override
		protected void onPostExecute(Boolean result) {
			dialogDone(result);
		}
	}
}