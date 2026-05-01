import { BrowserRouter, Routes, Route, Navigate }
  from 'react-router-dom';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import DashboardPage from './pages/DashboardPage';
import MachinePage from './pages/MachinePage';
import BookingPage from './pages/BookingPage';
import WalletPage from './pages/WalletPage';
import OAuth2CallbackPage from
  './pages/OAuth2CallbackPage';

function ProtectedRoute({
  children
}: {
  children: React.ReactNode
}) {
  const token = localStorage.getItem('token');
  if (!token) {
    return <Navigate to="/login" replace />;
  }
  return <>{children}</>;
}

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={
          <Navigate to="/login" replace />
        } />
            <Route path="/bookings" element={
         <ProtectedRoute>
          <BookingPage />
         </ProtectedRoute>
              } />
            <Route path="/wallet" element={
              <ProtectedRoute>
                <WalletPage />
              </ProtectedRoute>
              } />
        <Route path="/login"
               element={<LoginPage />} />
        <Route path="/register"
               element={<RegisterPage />} />
        <Route path="/oauth2/callback"
               element={<OAuth2CallbackPage />} />
        <Route path="/dashboard" element={
          <ProtectedRoute>
            <DashboardPage />
          </ProtectedRoute>
        } />

          <Route path="/wallet" element={
          <ProtectedRoute>
             <WalletPage />
          </ProtectedRoute>
            } />

        <Route path="/machines" element={
          <ProtectedRoute>
            <MachinePage />
          </ProtectedRoute>
        } />
      </Routes>
    </BrowserRouter>
  );
}

export default App;