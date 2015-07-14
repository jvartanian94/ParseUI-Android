package com.parse;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;

import bolts.Continuation;
import bolts.Task;

/**
 * A specialized {@code ImageView} that downloads and displays remote images stored on Parse's
 * servers.
 * <p>
 * Given a {@code ParseFile} storing an image, a {@code ParseImageView} works seamlessly to fetch
 * the file data and display it in the background. See below for an example:
 *
 * <pre>
 * ParseImageView imageView = (ParseImageView) findViewById(android.R.id.icon);
 * // The placeholder will be used before and during the fetch, to be replaced by the fetched image
 * // data.
 * imageView.setPlaceholder(getResources().getDrawable(R.drawable.placeholder));
 * imageView.setParseFile(file);
 * imageView.loadInBackground(new GetDataCallback() {
 *   &#064;Override
 *   public void done(byte[] data, ParseException e) {
 *     Log.i(&quot;ParseImageView&quot;,
 *         &quot;Fetched! Data length: &quot; + data.length + &quot;, or exception: &quot; + e.getMessage());
 *   }
 * });
 * </pre>
 */
public class ParseImageView extends ImageView {
  private ParseFile file;
  private Drawable placeholder;
  private boolean isLoaded = false;

  /**
   * Simple constructor to use when creating a {@code ParseImageView} from code.
   *
   * @param context
   *          Context for this View
   */
  public ParseImageView(Context context) {
    super(context);
  }

  /**
   * Constructor that is called when inflating a {@code ParseImageView} from XML.
   *
   * @param context
   *          Context for this View
   * @param attributeSet
   *          AttributeSet defined for this View in XML
   */
  public ParseImageView(Context context, AttributeSet attributeSet) {
    super(context, attributeSet);
  }

  /**
   * Perform inflation from XML and apply a class-specific base style.
   *
   * @param context
   *          Context for this View
   * @param attributeSet
   *          AttributeSet defined for this View in XML
   * @param defStyle
   *          Class-specific base style.
   */
  public ParseImageView(Context context, AttributeSet attributeSet, int defStyle) {
    super(context, attributeSet, defStyle);
  }

  @Override
  protected void onDetachedFromWindow() {
    // AdapterViews tend to try and avoid calling this, instead preferring to recycle the Views +
    // subviews. This is, however, called when the AdapterView itself is detached, or the Activity
    // is destroyed.
    if (this.file != null) {
      this.file.cancel();
    }
  }

  @Override
  public void setImageBitmap(Bitmap bitmap) {
    super.setImageBitmap(bitmap);
    this.isLoaded = true;
  }

  /**
   * Sets the placeholder to be used while waiting for an image to be loaded.
   *
   * @param placeholder
   *          A {@code Drawable} to be displayed while the remote image data is being fetched. This
   *          value can be null, and this {@code ImageView} will simply be blank while data is
   *          fetched.
   */
  public void setPlaceholder(Drawable placeholder) {
    this.placeholder = placeholder;
    if (!this.isLoaded) {
      this.setImageDrawable(this.placeholder);
    }
  }

  /**
   * Sets the remote file on Parse's server that stores the image.
   *
   * @param file
   *          The remote file on Parse's server.
   */
  public void setParseFile(ParseFile file) {
    if (this.file != null) {
      this.file.cancel();
    }
    this.isLoaded = false;
    this.file = file;
    this.setImageDrawable(this.placeholder);
  }

  /**
   * Kick off downloading of remote image. When the download is finished, the image data will be
   * displayed.
   *
   * @return A Task that is resolved when the image data is fetched and this View displays the image.
   */
  public Task<byte[]> loadInBackground() {
    if (file == null) {
      return Task.forResult(null);
    }

    final ParseFile loadingFile = file;
    return file.getDataInBackground().onSuccessTask(new Continuation<byte[], Task<byte[]>>() {
      @Override
      public Task<byte[]> then(Task<byte[]> task) throws Exception {
        byte[] data = task.getResult();
        if (file != loadingFile) {
          // This prevents the very slim chance of the file's download finishing and the callback
          // triggering just before this ImageView is reused for another ParseObject.
          return Task.cancelled();
        }
        if (data != null) {
          Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
          if (bitmap != null) {
            setImageBitmap(bitmap);
          }
        }
        return task;
      }
    }, Task.UI_THREAD_EXECUTOR);
  }

  /**
   * Kick off downloading of remote image. When the download is finished, the image data will be
   * displayed and the {@code completionCallback} will be triggered.
   *
   * @param completionCallback
   *          A custom {@code GetDataCallback} to be called after the image data is fetched and this
   *          {@code ImageView} displays the image.
   */
  public void loadInBackground(final GetDataCallback completionCallback) {
    ParseTaskUtils.callbackOnMainThreadAsync(loadInBackground(), completionCallback, true);
  }
}