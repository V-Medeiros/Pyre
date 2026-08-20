import { CloudContextProvider } from './context/CloudContext/CloudContextProvider';
import { TaskContextProvider } from './context/TaskContext/TaskContextProvider';
import { Home } from './pages/Home';

export function App() {
  return (
    <CloudContextProvider>
      <TaskContextProvider>
        <Home />
      </TaskContextProvider>
    </CloudContextProvider>
  );
}
