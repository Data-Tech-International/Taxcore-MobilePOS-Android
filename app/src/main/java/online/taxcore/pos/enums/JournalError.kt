package online.taxcore.pos.enums

import androidx.annotation.StringRes
import online.taxcore.pos.R

enum class JournalError(@StringRes val messageResId: Int) {
    WRONG_JSON_TYPE(R.string.error_wrong_json_type),
    INVALID_JSON_FORMAT(R.string.error_invalid_json_format),
    FILE_NOT_FOUND(R.string.error_file_not_found),
    UNABLE_TO_IMPORT(R.string.error_unable_to_import_journal)
}
