import { describe, expect, it } from "vitest";
import { readTaskFilters, toListParams, writeTaskFilters } from "./filters";
import { isTaskStatus, taskFormSchema, toTaskInput } from "./schemas";

describe("taskFormSchema", () => {
  it("accepts a valid task and trims its fields", () => {
    const result = taskFormSchema.parse({
      title: "  Write the README ",
      description: "  ",
      status: "IN_PROGRESS",
    });

    expect(result).toEqual({ title: "Write the README", description: "", status: "IN_PROGRESS" });
    expect(toTaskInput(result)).toEqual({
      title: "Write the README",
      description: null,
      status: "IN_PROGRESS",
    });
  });

  it.each([
    [{ title: "   ", description: "", status: "TODO" }, "title", "validation.titleRequired"],
    [{ title: "a".repeat(201), description: "", status: "TODO" }, "title", "validation.titleMax"],
    [
      { title: "Ok", description: "a".repeat(2001), status: "TODO" },
      "description",
      "validation.descriptionMax",
    ],
  ])("rejects %# with a translation key", (values, field, message) => {
    const result = taskFormSchema.safeParse(values);

    expect(result.success).toBe(false);
    expect(result.error?.issues[0]).toMatchObject({ path: [field], message });
  });

  it("only accepts the statuses of the API contract", () => {
    expect(isTaskStatus("DONE")).toBe(true);
    expect(isTaskStatus("ARCHIVED")).toBe(false);
    expect(taskFormSchema.safeParse({ title: "Ok", description: "", status: "x" }).success).toBe(
      false,
    );
  });
});

describe("task filters in the URL", () => {
  it("reads valid values and ignores invalid ones", () => {
    expect(readTaskFilters(new URLSearchParams("status=DONE&q=report&page=3"))).toEqual({
      status: "DONE",
      q: "report",
      page: 3,
    });
    expect(readTaskFilters(new URLSearchParams("status=NOPE&page=-2"))).toEqual({
      status: undefined,
      q: "",
      page: 1,
    });
  });

  it("writes canonical params, leaving defaults out", () => {
    const params = writeTaskFilters(new URLSearchParams("status=DONE&page=4"), {
      status: undefined,
      page: 1,
    });

    expect(params.toString()).toBe("");
  });

  it("converts to zero-based API params and drops a blank search", () => {
    expect(toListParams({ status: "TODO", q: "   ", page: 2 })).toEqual({
      status: "TODO",
      q: undefined,
      page: 1,
      size: 10,
    });
  });
});
