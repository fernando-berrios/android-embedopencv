package com.credera.example.embedopencv;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.lang.ref.WeakReference;

import butterknife.internal.DebouncingOnClickListener;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    static {
        if (!OpenCVLoader.initDebug()){
            Log.d(TAG, "Failed to load OpenCV :(");
        } else {
            Log.d(TAG, "Loaded OpenCV :)");
        }
    }

    private ImageView imageView;
    private Bitmap processedBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final Context context = this;

        imageView = (ImageView) findViewById(R.id.testImage);

        GlideApp.with(context)
                .load(R.drawable.test_image)
                .into(imageView);

        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        final DebouncingOnClickListener fabClickListener = new DebouncingOnClickListener() {
            Boolean fabToggle = true;

            @Override
            public void doClick(View v) {
                if (!fabToggle) {
                    fabToggle = true;

                    fab.setImageResource(android.R.drawable.ic_menu_edit);

                    GlideApp.with(context)
                            .load(R.drawable.test_image)
                            .into(imageView);

                    return;
                }

                fabToggle = false;
                fab.setImageResource(android.R.drawable.ic_menu_gallery);

                new ConvertToGrayAsyncTask(context, imageView).execute();
            }
        };

        fab.setOnClickListener(fabClickListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public static Bitmap decodeSampledBitmapFromResource(Resources res,
                                                         int resId,
                                                         int reqWidth,
                                                         int reqHeight) {

        // First decode with inJustDecodeBounds = true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res, resId, options);
    }

    public static int calculateInSampleSize(BitmapFactory.Options options,
                                            int reqWidth,
                                            int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    private class ConvertToGrayAsyncTask extends AsyncTask<Void, Void, Bitmap> {

        private WeakReference<Context> contextRef;
        private WeakReference<ImageView> imageViewRef;

        ConvertToGrayAsyncTask(Context context, ImageView imageView) {
            contextRef = new WeakReference<>(context);
            imageViewRef = new WeakReference<>(imageView);
        }

        @Override
        protected void onPostExecute(final Bitmap bitmap) {
            super.onPostExecute(bitmap);

            if (bitmap == null || contextRef.get() == null || imageViewRef.get() == null) {
                return;
            }

            Context context = contextRef.get();

            GlideApp.with(context)
                    .load(bitmap)
                    .into(imageViewRef.get());
        }

        @Override
        protected Bitmap doInBackground(Void... voids) {
            if (contextRef.get() == null || imageViewRef.get() == null) {
                return null;
            }

            Context context = contextRef.get();
            ImageView imageView = imageViewRef.get();

            if (processedBitmap != null) {
                processedBitmap.recycle();
            }

            final Bitmap src = decodeSampledBitmapFromResource(context.getResources(),
                    R.drawable.test_image,
                    imageView.getWidth(),
                    imageView.getHeight());

            Mat image = new Mat();
            Utils.bitmapToMat(src, image);

            src.recycle();

            Mat grayMat = new Mat();
            Imgproc.cvtColor(image, grayMat, Imgproc.COLOR_BGR2GRAY, CvType.CV_32S);

            processedBitmap = Bitmap.createBitmap(grayMat.cols(),
                    grayMat.rows(),
                    Bitmap.Config.ARGB_8888);

            Utils.matToBitmap(grayMat, processedBitmap);

            return processedBitmap;
        }
    }

}
