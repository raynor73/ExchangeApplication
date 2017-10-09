package ru.ilapin.exchangeapplication.backend;

public class Result<T> {

	private T mData;
	private boolean mHasError;

	public Result() {}

	public Result(final T data, final boolean hasError) {
		mData = data;
		mHasError = hasError;
	}

	public T getData() {
		return mData;
	}

	public void setData(final T data) {
		mData = data;
	}

	public boolean hasError() {
		return mHasError;
	}

	public void setHasError(final boolean hasError) {
		mHasError = hasError;
	}
}
