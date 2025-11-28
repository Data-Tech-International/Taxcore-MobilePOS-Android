package online.taxcore.pos.enums

import androidx.annotation.StringRes
import online.taxcore.pos.R

enum class CatalogError(@StringRes val messageResId: Int) {
    WRONG_CSV_TYPE(R.string.error_wrong_csv_type),
    FILE_NOT_FOUND(R.string.error_file_not_found),
    WRONG_DATA_TYPE(R.string.error_wrong_data_type),
    INVALID_FILE_CONTENT(R.string.error_invalid_file_content),
    UNABLE_TO_COMPLETE(R.string.error_unable_to_complete),
    NOT_ENOUGH_SPACE(R.string.error_not_enough_space),
    EXPORT_FILE_NOT_FOUND(R.string.toast_export_file_not_found),
    UNABLE_TO_EXPORT(R.string.error_unable_to_export),
    WRONG_JSON_TYPE(R.string.error_wrong_json_type)
}
