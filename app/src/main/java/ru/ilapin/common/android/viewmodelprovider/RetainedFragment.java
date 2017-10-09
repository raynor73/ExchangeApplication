package ru.ilapin.common.android.viewmodelprovider;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import java.util.*;

public class RetainedFragment extends Fragment {

	private final Map<Class, ViewModel> mViewModels = new HashMap<>();

	@Override
	public void onCreate(@Nullable final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setRetainInstance(true);
	}

	void putViewModel(final ViewModel viewModel) {
		mViewModels.put(viewModel.getClass(), viewModel);
	}

	@SuppressWarnings("unchecked")
	<T extends ViewModel> T findViewModel(final Class<T> type) {
		return (T) mViewModels.get(type);
	}

	Collection<ViewModel> getViewModles() {
		return mViewModels.values();
	}
}
