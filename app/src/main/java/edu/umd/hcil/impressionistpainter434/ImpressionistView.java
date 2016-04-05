package edu.umd.hcil.impressionistpainter434;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.text.MessageFormat;
import java.util.Random;

/**
 * Created by jon on 3/20/2016.
 */
public class ImpressionistView extends View {

    private ImageView _imageView;

    private Canvas _offScreenCanvas = null;
    private Bitmap _offScreenBitmap = null;
    private Paint _paint = new Paint();

    private int _alpha = 150;
    private int _defaultRadius = 25;
    private Point _lastPoint = null;
    private long _lastPointTime = -1;
    private boolean _useMotionSpeedForBrushStrokeSize = true;
    private Paint _paintBorder = new Paint();
    private BrushType _brushType = BrushType.Square;
    private float _minBrushRadius = 5;
    private VelocityTracker _velocityTracker = null;

    public ImpressionistView(Context context) {
        super(context);
        init(null, 0);
    }

    public ImpressionistView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public ImpressionistView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    /**
     * Because we have more than one constructor (i.e., overloaded constructors), we use
     * a separate initialization method
     * @param attrs
     * @param defStyle
     */
    private void init(AttributeSet attrs, int defStyle){

        // Set setDrawingCacheEnabled to true to support generating a bitmap copy of the view (for saving)
        // See: http://developer.android.com/reference/android/view/View.html#setDrawingCacheEnabled(boolean)
        //      http://developer.android.com/reference/android/view/View.html#getDrawingCache()
        this.setDrawingCacheEnabled(true);

        _paint.setColor(Color.RED);
        _paint.setAlpha(_alpha);
        _paint.setAntiAlias(true);
        _paint.setStyle(Paint.Style.FILL);
        _paint.setStrokeWidth(4);

        _paintBorder.setColor(Color.BLACK);
        _paintBorder.setStrokeWidth(3);
        _paintBorder.setStyle(Paint.Style.STROKE);
        _paintBorder.setAlpha(50);

        //_paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY));
    }

    @Override
    protected void onSizeChanged (int w, int h, int oldw, int oldh){

        Bitmap bitmap = getDrawingCache();
        Log.v("onSizeChanged", MessageFormat.format("bitmap={0}, w={1}, h={2}, oldw={3}, oldh={4}", bitmap, w, h, oldw, oldh));
        if(bitmap != null) {
            _offScreenBitmap = getDrawingCache().copy(Bitmap.Config.ARGB_8888, true);
            _offScreenCanvas = new Canvas(_offScreenBitmap);
        }
    }

    /**
     * Sets the ImageView, which hosts the image that we will paint in this view
     * @param imageView
     */
    public void setImageView(ImageView imageView){
        _imageView = imageView;
    }

    /**
     * Sets the brush type. Feel free to make your own and completely change my BrushType enum
     * @param brushType
     */
    public void setBrushType(BrushType brushType){
        _brushType = brushType;
    }

    /**
     * Clears the painting
     */
    public void clearPainting(){
        //TODO
        _offScreenCanvas.drawColor(Color.WHITE);
        invalidate();
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(_offScreenBitmap != null) {
            canvas.drawBitmap(_offScreenBitmap, 0, 0, _paint);
        }

        // Draw the border. Helpful to see the size of the bitmap in the ImageView
        canvas.drawRect(getBitmapPositionInsideImageView(_imageView), _paintBorder);
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent){
        //TODO
        //Basically, the way this works is to listen for Touch Down and Touch Move events and determine where those
        //touch locations correspond to the bitmap in the ImageView. You can then grab info about the bitmap--like the pixel color--
        //at that location
        float touchX = motionEvent.getX();
        float touchY = motionEvent.getY();

        //this prevents crashing from clicking outside of boundaries
        if (touchX < 0 || touchX > _offScreenBitmap.getWidth() || touchY < 0 || touchY > _offScreenBitmap.getHeight()) {
            return false;
        }

        //these get the ratio of the touch point to the view so that it can be used for
        //images of different scaling.
        float xRatio = touchX/_offScreenBitmap.getWidth();
        float yRatio = touchY/_offScreenBitmap.getHeight();

        //get the bitmap from the imageview and get the pixel according to the ratio of the touch
        if(_imageView.getDrawable() == null) { // return false if no image picked yet
            return false;
        }

        Bitmap imageViewBitmap = ((BitmapDrawable)_imageView.getDrawable()).getBitmap();
        int pixel = imageViewBitmap.getPixel(Math.round(xRatio * imageViewBitmap.getWidth()),Math.round(yRatio * imageViewBitmap.getHeight()));
        int r = Color.red(pixel);
        int g = Color.green(pixel);
        int b = Color.blue(pixel);
        int alpha = Color.alpha(pixel);
        _paint.setColor(Color.argb(alpha,r,g,b));

        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if(_brushType == BrushType.Square) {
                    _offScreenCanvas.drawRect(touchX - 20, touchY - 20, touchX + 20, touchY + 20, _paint);
                } else if(_brushType == BrushType.Circle) {
                    _offScreenCanvas.drawCircle(touchX, touchY, 10, _paint);
                } else if(_brushType == BrushType.Line) {
                    _offScreenCanvas.drawLine(touchX, touchY-30, touchX, touchY+30, _paint);
                } else if(_brushType == BrushType.Eraser) {
                    _paint.setColor(Color.WHITE);
                    _offScreenCanvas.drawCircle(touchX, touchY, 40, _paint);
                }

                //set velocity tracker
                if(_velocityTracker == null) {
                    _velocityTracker = VelocityTracker.obtain();
                } else {
                    _velocityTracker.clear();
                }
                _velocityTracker.addMovement(motionEvent);

                break;
            case MotionEvent.ACTION_MOVE:
                if(_brushType == BrushType.Square) {
                    _offScreenCanvas.drawRect(touchX - 20, touchY - 20, touchX + 20, touchY + 20, _paint);
                } else if(_brushType == BrushType.Circle) {
                    //calculate velocity for circle
                    _velocityTracker.addMovement(motionEvent);
                    _velocityTracker.computeCurrentVelocity(1000);
                    float xVelocity = Math.abs(_velocityTracker.getXVelocity());
                    float yVelocity = Math.abs(_velocityTracker.getYVelocity());
                    float velocity = ((xVelocity + yVelocity)/2) / 65; //adjust velocity to scale with size

                    if(velocity < 10) { //if velocity is too low, make minimum radius 10
                        velocity = 10;
                    }

                    _offScreenCanvas.drawCircle(touchX, touchY, velocity, _paint);
                } else if(_brushType == BrushType.Line) {
                    _paint.setStrokeWidth(13);
                    _offScreenCanvas.drawLine(touchX, touchY-45, touchX, touchY+45, _paint);
                } else if(_brushType == BrushType.Eraser) {
                    _paint.setColor(Color.WHITE);
                    _offScreenCanvas.drawCircle(touchX, touchY, 40, _paint);
                }

                break;
            case MotionEvent.ACTION_UP:
                break;
            default:
                return false;
        }

        invalidate();
        return true;
    }

    //save image
    public void saveImage() {
        File externalStorageDirFile = Environment.getExternalStorageDirectory();
        String externalStorageDirStr = Environment.getExternalStorageDirectory().getAbsolutePath();
        String guessedFilename = URLUtil.guessFileName("image", null, null);
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), guessedFilename);
        try {
            boolean compressSucceeded = _offScreenBitmap.compress(Bitmap.CompressFormat.PNG, 100, new FileOutputStream(file));
            FileUtils.addImageToGallery(file.getAbsolutePath(), this.getContext());
            Toast.makeText(this.getContext(), "Saved to " + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }


    /**
     * This method is useful to determine the bitmap position within the Image View. It's not needed for anything else
     * Modified from:
     *  - http://stackoverflow.com/a/15538856
     *  - http://stackoverflow.com/a/26930938
     * @param imageView
     * @return
     */
    private static Rect getBitmapPositionInsideImageView(ImageView imageView){
        Rect rect = new Rect();

        if (imageView == null || imageView.getDrawable() == null) {
            return rect;
        }

        // Get image dimensions
        // Get image matrix values and place them in an array
        float[] f = new float[9];
        imageView.getImageMatrix().getValues(f);

        // Extract the scale values using the constants (if aspect ratio maintained, scaleX == scaleY)
        final float scaleX = f[Matrix.MSCALE_X];
        final float scaleY = f[Matrix.MSCALE_Y];

        // Get the drawable (could also get the bitmap behind the drawable and getWidth/getHeight)
        final Drawable d = imageView.getDrawable();
        final int origW = d.getIntrinsicWidth();
        final int origH = d.getIntrinsicHeight();

        // Calculate the actual dimensions
        final int widthActual = Math.round(origW * scaleX);
        final int heightActual = Math.round(origH * scaleY);

        // Get image position
        // We assume that the image is centered into ImageView
        int imgViewW = imageView.getWidth();
        int imgViewH = imageView.getHeight();

        int top = (int) (imgViewH - heightActual)/2;
        int left = (int) (imgViewW - widthActual)/2;

        rect.set(left, top, left + widthActual, top + heightActual);

        return rect;
    }
}

