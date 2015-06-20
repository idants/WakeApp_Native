package com.example.wakeapp;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.RectF;
import android.os.Build;
import android.text.Layout.Alignment;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.SparseIntArray;
import android.util.TypedValue;
import android.widget.TextView;

public class AutoResizeTextView extends TextView {
private interface SizeTester {
    /**
     * 
     * @param suggestedSize
     *            Size of text to be tested
     * @param availableSpace
     *            available space in which text must fit
     * @return an integer < 0 if after applying {@code suggestedSize} to
     *         text, it takes less space than {@code availableSpace}, > 0
     *         otherwise
     */
    public int onTestSize(int suggestedSize, RectF availableSpace);
}

private RectF mTextRect = new RectF();

private RectF mAvailableSpaceRect;

private SparseIntArray mTextCachedSizes;

private TextPaint mPaint;

private float mMaxTextSize;

private float mSpacingMult = 1.0f;

private float mSpacingAdd = 0.0f;

private float mMinTextSize = 20;

private int mWidthLimit;

private static final int NO_LINE_LIMIT = -1;
private static final int MAX_ATTEMPTS = 3; 
private static final int MIN_TEXT_SIZE = 24;
private int mMaxLines;
private boolean preventReajust = false; //to avoid stack overflow

private boolean mEnableSizeCache = true;
private boolean mInitiallized;

public AutoResizeTextView(Context context) {
    super(context);
    initialize();
}

public AutoResizeTextView(Context context, AttributeSet attrs) {
    super(context, attrs);
    initialize();
}

public AutoResizeTextView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    initialize();
}

private void initialize() {
    mPaint = new TextPaint(getPaint());
    mMaxTextSize = getTextSize();
    mAvailableSpaceRect = new RectF();
    mTextCachedSizes = new SparseIntArray();
    if (mMaxLines == 0) {
        // no value was assigned during construction
        mMaxLines = NO_LINE_LIMIT;
    }
    mInitiallized = true;
}

@Override
public void setText(final CharSequence text, BufferType type) {
    super.setText(text, type);
    adjustTextSize(text.toString());
}

@Override
public void setTextSize(float size) {
    mMaxTextSize = size;
    mTextCachedSizes.clear();
    adjustTextSize(getText().toString());
}

@Override
public void setMaxLines(int maxlines) {
    super.setMaxLines(maxlines);
    mMaxLines = maxlines;
    if (!preventReajust){
    	reAdjust();
    }
}

public int getMaxLines() {
    return mMaxLines;
}

@Override
public void setSingleLine() {
    super.setSingleLine();
    mMaxLines = 1;
    if (!preventReajust){
    	reAdjust();
    }
}

@Override
public void setSingleLine(boolean singleLine) {
    super.setSingleLine(singleLine);
    if (singleLine) {
        mMaxLines = 1;
    } else {
        mMaxLines = NO_LINE_LIMIT;
    }
    if (!preventReajust){
    	reAdjust();
    }
}

@Override
public void setLines(int lines) {
    super.setLines(lines);
    mMaxLines = lines;
    if (!preventReajust){
    	reAdjust();
    }
}

@Override
public void setTextSize(int unit, float size) {
    Context c = getContext();
    Resources r;

    if (c == null)
        r = Resources.getSystem();
    else
        r = c.getResources();
    mMaxTextSize = TypedValue.applyDimension(unit, size,
            r.getDisplayMetrics());
    mTextCachedSizes.clear();
    adjustTextSize(getText().toString());
}

@Override
public void setLineSpacing(float add, float mult) {
    super.setLineSpacing(add, mult);
    mSpacingMult = mult;
    mSpacingAdd = add;
}

/**
 * Set the lower text size limit and invalidate the view
 * 
 * @param minTextSize
 */
public void setMinTextSize(float minTextSize) {
    mMinTextSize = minTextSize;
    if (!preventReajust){
    	reAdjust();
    }
}

private void reAdjust() {
    adjustTextSize(getText().toString());
}

private void adjustTextSize(String string) {
    if (!mInitiallized) {
        return;
    }
    int startSize = (int) mMinTextSize;
    int heightLimit = getMeasuredHeight() - getCompoundPaddingBottom()
            - getCompoundPaddingTop();
    mWidthLimit = getMeasuredWidth() - getCompoundPaddingLeft()
            - getCompoundPaddingRight();
    mAvailableSpaceRect.right = mWidthLimit;
    mAvailableSpaceRect.bottom = heightLimit;
    int bestTextSize = efficientTextSizeSearch(startSize, (int) mMaxTextSize, mSizeTester, mAvailableSpaceRect);

	preventReajust = true;
	super.setSingleLine(false);
	super.setEllipsize(TextUtils.TruncateAt.END);
	super.setLines(12);
    
	preventReajust = false;
    
    super.setTextSize(TypedValue.COMPLEX_UNIT_PX, bestTextSize);
}

private final SizeTester mSizeTester = new SizeTester() {
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public int onTestSize(int suggestedSize, RectF availableSPace) {
        mPaint.setTextSize(suggestedSize);
        String text = getText().toString();
        boolean singleline = getMaxLines() == 1;
        if (singleline) {
            mTextRect.bottom = mPaint.getFontSpacing();
            mTextRect.right = mPaint.measureText(text);
        } else {
            StaticLayout layout = new StaticLayout(text, mPaint,
                    mWidthLimit, Alignment.ALIGN_NORMAL, mSpacingMult,
                    mSpacingAdd, true);
            // return early if we have more lines
            if (getMaxLines() != NO_LINE_LIMIT
                    && layout.getLineCount() > getMaxLines()) {
                return 1;
            }
            mTextRect.bottom = layout.getHeight();
            int maxWidth = -1;
            for (int i = 0; i < layout.getLineCount(); i++) {
                if (maxWidth < layout.getLineWidth(i)) {
                    maxWidth = (int) layout.getLineWidth(i);
                }
            }
            mTextRect.right = maxWidth;
        }

        mTextRect.offsetTo(0, 0);
        if (availableSPace.contains(mTextRect)) {
            // may be too small, don't worry we will find the best match
            return -1;
        } else {
            // too big
            return 1;
        }
    }
};

/**
 * Enables or disables size caching, enabling it will improve performance
 * where you are animating a value inside TextView. This stores the font
 * size against getText().length() Be careful though while enabling it as 0
 * takes more space than 1 on some fonts and so on.
 * 
 * @param enable
 *            enable font size caching
 */
public void enableSizeCache(boolean enable) {
    mEnableSizeCache = enable;
    mTextCachedSizes.clear();
    adjustTextSize(getText().toString());
}

private int efficientTextSizeSearch(int start, int end, SizeTester sizeTester, RectF availableSpace) {
    if (!mEnableSizeCache) {
        return binarySearch(start, end, sizeTester, availableSpace);
    }
    String text = getText().toString();
    int key = text == null ? 0 : text.length();
    int size = mTextCachedSizes.get(key);
    if (size != 0) {
        return size;
    }
    size = binarySearch(start, end, sizeTester, availableSpace);
    mTextCachedSizes.put(key, size);
    return size;
}

private static int binarySearch(int start, int end, SizeTester sizeTester, RectF availableSpace) {
    int mid = end;
    int midValCmp;
    
    for (int i=0; i < MAX_ATTEMPTS; i++){
    	midValCmp = sizeTester.onTestSize(mid, availableSpace);
    	if (midValCmp < 0){
    		return mid;
    	}
    	else{
    		mid -= 2;
    	}
    }
    
    return mid;
}

@Override
protected void onTextChanged(final CharSequence text, final int start,
        final int before, final int after) {
    super.onTextChanged(text, start, before, after);
    reAdjust();
}

@Override
protected void onSizeChanged(int width, int height, int oldwidth,
        int oldheight) {
    mTextCachedSizes.clear();
    super.onSizeChanged(width, height, oldwidth, oldheight);
    if ((width != oldwidth || height != oldheight) && !preventReajust) {
        reAdjust();
    }
}
}