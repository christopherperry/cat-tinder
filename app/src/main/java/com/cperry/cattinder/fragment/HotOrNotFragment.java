package com.cperry.cattinder.fragment;

import android.content.Context;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.cperry.cattinder.R;
import com.cperry.cattinder.api.CatImageService;
import com.cperry.cattinder.api.ServiceFactory;
import com.cperry.cattinder.api.ServiceFactoryImpl;
import com.cperry.cattinder.data.Cats.Cat;
import com.lorentzos.flingswipe.FlingCardListener;
import com.lorentzos.flingswipe.SwipeFlingAdapterView;
import com.squareup.picasso.Picasso;

import java.util.List;

import rx.android.observables.AndroidObservable;
import rx.schedulers.Schedulers;
import timber.log.Timber;

public class HotOrNotFragment extends BaseFragment {

  @Nullable @Override public View onCreateView(
    LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_hotornot, container, false);
  }

  @Override public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);

    ServiceFactory serviceFactory = new ServiceFactoryImpl();
    CatImageService catImageService = serviceFactory.getCatImageService();

    AndroidObservable.bindFragment(this, catImageService.getCats())
      .subscribeOn(Schedulers.io())
      .subscribe(cats -> {
        showCats(cats.get());
      });
  }

  private void showCats(List<Cat> cats) {
    KittyAdapter adapter = new KittyAdapter(
      LayoutInflater.from(getActivity()),
      Picasso.with(getActivity()),
      cats
    );

    SwipeFlingAdapterView stackView = findViewById(R.id.kittyStack);
    stackView.setFlingListener(new SwipeFlingAdapterView.onFlingListener() {
      @Override public void removeFirstObjectInAdapter() {
        cats.remove(0);
        adapter.notifyDataSetChanged();
      }

      @Override public void onLeftCardExit(Object o) {
        Timber.d("onLeftCardExit: " + o);
      }

      @Override public void onRightCardExit(Object o) {
        Timber.d("onRightCardExit: " + o);
      }

      @Override public void onAdapterAboutToEmpty(int i) { }

      @Override public void onScroll(float v) {
        // Negative values means left, positive means right
        Timber.d("onScroll: " + v);
        View view = stackView.getSelectedView();
        if (v < 0) {
          view.findViewById(R.id.no).setAlpha(v * -0.8f);
        } else if (v > 0) {
          view.findViewById(R.id.yes).setAlpha(v * 0.8f);
        } else {
          view.findViewById(R.id.no).setAlpha(0);
          view.findViewById(R.id.yes).setAlpha(0);
        }
      }
    });

    stackView.setAdapter(adapter);

    // Won't show anything if you don't call this
    // I think it's an implementation bug in the SwipeFlingAdapterView
    stackView.requestLayout();

    // YES/NO Button clicks
    findViewById(R.id.noButton).setOnClickListener(v -> stackView.getTopCardListener().selectLeft());
    findViewById(R.id.yesButton).setOnClickListener(v -> stackView.getTopCardListener().selectRight());
  }

  static class KittyAdapter extends BaseAdapter {
    private final LayoutInflater inflater;
    private final Picasso picasso;
    private final List<Cat> cats;

    KittyAdapter(LayoutInflater inflater, Picasso picasso, List<Cat> cats) {
      this.inflater = inflater;
      this.picasso = picasso;
      this.cats = cats;
    }

    @Override public int getCount() {
      return cats.size();
    }

    @Override public Cat getItem(int position) {
      return cats.get(position);
    }

    @Override public long getItemId(int position) {
      return position;
    }

    @Override public View getView(int position, View convertView, ViewGroup parent) {
      View view = convertView;
      if (view == null) {
        view = inflater.inflate(R.layout.kitty_cat, parent, false);
      }

      Cat cat = getItem(position);

      int size = (int) dipsToPixels(view.getContext(), 290f);
      picasso.load(cat.getUri())
        .resize(size, size)
        .centerCrop()
        .noFade()
        .into((ImageView) view.findViewById(R.id.image));

      ((TextView) view.findViewById(R.id.snippet)).setText(cat.getSnippet());

      return view;
    }

    float dipsToPixels(Context context, float dipValue) {
      DisplayMetrics metrics = context.getResources().getDisplayMetrics();
      return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dipValue, metrics);
    }
  }
}
