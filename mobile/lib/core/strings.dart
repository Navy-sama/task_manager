/// All user-facing copy lives here so the (currently English-only) UI text
/// has one place to grow into real i18n later.
class Strings {
  const Strings._();

  static const appTitle = 'Task Manager';

  // Auth
  static const email = 'Email';
  static const password = 'Password';
  static const confirmPassword = 'Confirm password';
  static const login = 'Log in';
  static const register = 'Create account';
  static const logout = 'Log out';
  static const noAccountYet = "Don't have an account?";
  static const alreadyHaveAccount = 'Already have an account?';
  static const createAccount = 'Create an account';
  static const backToLogin = 'Back to login';
  static const showPassword = 'Show password';
  static const hidePassword = 'Hide password';
  static const emailRequired = 'Enter your email.';
  static const emailInvalid = 'Enter a valid email address.';
  static const passwordRequired = 'Enter your password.';
  static const passwordLength = 'Password must be 8 to 72 characters.';
  static const confirmPasswordMismatch = 'Passwords do not match.';

  // Tasks
  static const tasks = 'Tasks';
  static const searchTasks = 'Search tasks';
  static const filterAll = 'All';
  static const filterTodo = 'To do';
  static const filterInProgress = 'In progress';
  static const filterDone = 'Done';
  static const noTasks = 'No tasks yet. Tap + to create one.';
  static const noTasksMatch = 'No tasks match your search.';
  static const newTask = 'New task';
  static const editTask = 'Edit task';
  static const title = 'Title';
  static const description = 'Description (optional)';
  static const status = 'Status';
  static const save = 'Save';
  static const delete = 'Delete task';
  static const deleteConfirmTitle = 'Delete this task?';
  static const deleteConfirmBody = 'This cannot be undone.';
  static const cancel = 'Cancel';
  static const titleRequired = 'Enter a title.';
  static const somethingWentWrong = 'Something went wrong. Please try again.';
  static const retry = 'Retry';
}
