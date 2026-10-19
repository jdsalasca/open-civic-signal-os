import { useEffect } from "react";
import { Controller, useForm } from "react-hook-form";
import { Dialog } from "primereact/dialog";
import { InputTextarea } from "primereact/inputtextarea";
import { classNames } from "primereact/utils";
import { useTranslation } from "react-i18next";
import { CivicButton } from "../ui/CivicButton";
import { CivicCharacterCount } from "../ui/CivicCharacterCount";
import { CivicField } from "../ui/CivicField";
import { CivicSelect } from "../ui/CivicSelect";
import type { CommunityModerationReasonCode } from "../../types";

type ModerationReportForm = {
  reasonCode: CommunityModerationReasonCode;
  details: string;
};

type Props = {
  visible: boolean;
  submitting: boolean;
  targetLabel: string;
  onHide: () => void;
  onSubmit: (values: ModerationReportForm) => Promise<void> | void;
};

const DETAIL_MIN = 8;
const DETAIL_MAX = 2000;

const defaultValues: ModerationReportForm = {
  reasonCode: "ABUSE",
  details: "",
};

export function ModerationReportDialog({ visible, submitting, targetLabel, onHide, onSubmit }: Props) {
  const { t } = useTranslation();
  const {
    control,
    handleSubmit,
    reset,
    watch,
    formState: { errors },
  } = useForm<ModerationReportForm>({
    mode: "onChange",
    defaultValues,
  });

  const details = watch("details") ?? "";

  useEffect(() => {
    if (visible) {
      reset(defaultValues);
    }
  }, [reset, visible]);

  const reasonOptions = [
    { label: t("moderation.reason_codes.ABUSE"), value: "ABUSE" },
    { label: t("moderation.reason_codes.HARASSMENT"), value: "HARASSMENT" },
    { label: t("moderation.reason_codes.SPAM"), value: "SPAM" },
    { label: t("moderation.reason_codes.MISINFORMATION"), value: "MISINFORMATION" },
    { label: t("moderation.reason_codes.OFF_TOPIC"), value: "OFF_TOPIC" },
    { label: t("moderation.reason_codes.OTHER"), value: "OTHER" },
  ] as Array<{ label: string; value: CommunityModerationReasonCode }>;

  return (
    <Dialog
      header={<div className="text-xl font-black text-main">{t("moderation.report_dialog.title")}</div>}
      visible={visible}
      onHide={onHide}
      className="w-full max-w-30rem mx-3"
      breakpoints={{ "960px": "75vw", "641px": "94vw" }}
    >
      <form className="flex flex-column gap-3 pt-2" onSubmit={handleSubmit(async (values) => onSubmit(values))}>
        <div className="u-surface-note">
          <div className="u-eyebrow mb-2">{t("moderation.report_dialog.target_label")}</div>
          <p className="text-sm text-secondary m-0 line-height-3">{targetLabel}</p>
        </div>

        <CivicField label={t("moderation.report_dialog.reason_label")} error={errors.reasonCode?.message}>
          <Controller
            name="reasonCode"
            control={control}
            rules={{ required: t("common.required") }}
            render={({ field, fieldState }) => (
              <CivicSelect
                value={field.value}
                onChange={(e) => field.onChange(e.value)}
                options={reasonOptions}
                className={classNames("w-full", { "p-invalid": fieldState.error })}
                data-testid="moderation-report-reason-select"
              />
            )}
          />
        </CivicField>

        <CivicField
          label={t("moderation.report_dialog.details_label")}
          error={errors.details?.message}
          helpText={t("moderation.report_dialog.details_help")}
        >
          <Controller
            name="details"
            control={control}
            rules={{
              required: t("common.required"),
              minLength: { value: DETAIL_MIN, message: t("moderation.report_dialog.details_too_short") },
              maxLength: { value: DETAIL_MAX, message: t("moderation.report_dialog.details_too_long") },
            }}
            render={({ field, fieldState }) => (
              <div className="flex flex-column gap-2">
                <InputTextarea
                  {...field}
                  rows={5}
                  onChange={(e) => field.onChange(e.target.value)}
                  className={classNames("w-full", { "p-invalid": fieldState.error })}
                  maxLength={DETAIL_MAX}
                  data-testid="moderation-report-details-input"
                  placeholder={t("moderation.report_dialog.details_placeholder")}
                />
                <CivicCharacterCount current={details.length} max={DETAIL_MAX} min={DETAIL_MIN} />
              </div>
            )}
          />
        </CivicField>

        <div className="flex justify-content-end gap-2 mt-2">
          <CivicButton type="button" variant="ghost" label={t("common.cancel")} onClick={onHide} />
          <CivicButton type="submit" icon="pi pi-flag" label={t("moderation.report_dialog.submit")} loading={submitting} data-testid="moderation-report-submit-button" />
        </div>
      </form>
    </Dialog>
  );
}
