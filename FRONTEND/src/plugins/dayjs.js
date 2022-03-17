import dayjs from "dayjs";
import relativeTime from "dayjs/plugin/relativeTime";
import "dayjs/locale/ko";

dayjs.locale("ko");
dayjs.extend(relativeTime);

const fromNow = (dateTime) => {
  const diff = dayjs().diff(dateTime, "days");
  if (diff < 1) {
    return "오늘";
  }
  if (diff < 7) {
    return dayjs(dateTime).fromNow();
  }
  return dayjs(dateTime).format("YYYY-MM-DD");
};

const checkEnd = (dateTime) => {
  const diff = dayjs().diff(dateTime, "days");
  if (diff > 0) {
    return true;
  }
  return false;
};

export { fromNow, checkEnd };
