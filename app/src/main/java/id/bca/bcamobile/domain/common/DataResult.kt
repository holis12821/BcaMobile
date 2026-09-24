package id.bca.bcamobile.domain.common

import id.bca.bcamobile.core.network.ApiFailure

/** Hasil pemanggilan repository: sukses membawa nilai, gagal membawa error terklasifikasi. */
sealed interface DataResult<out T> {
    data class Success<T>(val value: T) : DataResult<T>
    data class Failure(val error: ApiFailure) : DataResult<Nothing>
}

inline fun <T> DataResult<T>.onSuccess(action: (T) -> Unit): DataResult<T> {
    if (this is DataResult.Success) action(value)
    return this
}

inline fun <T> DataResult<T>.onFailure(action: (ApiFailure) -> Unit): DataResult<T> {
    if (this is DataResult.Failure) action(error)
    return this
}

/** Ubah nilai sukses; kegagalan diteruskan apa adanya. */
inline fun <T, R> DataResult<T>.map(transform: (T) -> R): DataResult<R> = when (this) {
    is DataResult.Success -> DataResult.Success(transform(value))
    is DataResult.Failure -> this
}
